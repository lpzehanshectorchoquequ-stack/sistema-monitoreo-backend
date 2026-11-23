package com.integrador.sistema_monitoreo_backend.application;

import com.integrador.sistema_monitoreo_backend.domain.AccesoDenegadoException;
import com.integrador.sistema_monitoreo_backend.domain.Cita;
import com.integrador.sistema_monitoreo_backend.domain.CitaRepository;
import com.integrador.sistema_monitoreo_backend.domain.HorarioAtencion;
import com.integrador.sistema_monitoreo_backend.domain.HorarioRepository;
import com.integrador.sistema_monitoreo_backend.domain.Paciente;
import com.integrador.sistema_monitoreo_backend.domain.PacienteRepository;
import com.integrador.sistema_monitoreo_backend.domain.RecursoNoEncontradoException;
import com.integrador.sistema_monitoreo_backend.domain.ReglaNegocioException;
import com.integrador.sistema_monitoreo_backend.domain.Usuario;
import com.integrador.sistema_monitoreo_backend.domain.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GestionarCitasUseCase {

    public record HorarioVista(Integer id, int diaSemana, String horaInicio, String horaFin, int duracionMinutos) {}

    public record MisHorariosVista(BigDecimal tarifaConsulta, List<HorarioVista> horarios) {}

    public record EspacioVista(String fecha, String horaInicio, String horaFin) {}

    public record CitaVista(Integer id, Integer pacienteId, String pacienteNombre, String psicologoNombre,
                            String solicitanteNombre, String solicitanteRol, String fecha, String horaInicio,
                            String horaFin, String motivo, boolean urgente, String estado, BigDecimal costo,
                            String comentarioPsicologa, List<String> acciones) {}

    private static final String SOLICITADA = "SOLICITADA";
    private static final String PROPUESTA = "PROPUESTA";
    private static final String CONFIRMADA = "CONFIRMADA";
    private static final String RECHAZADA = "RECHAZADA";
    private static final String CANCELADA = "CANCELADA";
    private static final String ATENDIDA = "ATENDIDA";
    private static final String NO_ASISTIO = "NO_ASISTIO";

    private static final List<String> ESTADOS_ACTIVOS = List.of(SOLICITADA, PROPUESTA, CONFIRMADA);
    private static final int DIAS_VISIBLES = 14;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final CitaRepository citaRepository;
    private final HorarioRepository horarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final GestionarPacientesUseCase pacientes;

    public GestionarCitasUseCase(CitaRepository citaRepository,
                                 HorarioRepository horarioRepository,
                                 UsuarioRepository usuarioRepository,
                                 PacienteRepository pacienteRepository,
                                 GestionarPacientesUseCase pacientes) {
        this.citaRepository = citaRepository;
        this.horarioRepository = horarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.pacientes = pacientes;
    }

    // ---------- Horarios de la psicóloga ----------

    @Transactional(readOnly = true)
    public MisHorariosVista misHorarios(String correo) {
        Usuario psicologa = usuario(correo);
        return new MisHorariosVista(psicologa.getTarifaConsulta(),
                horarioRepository.listarPorPsicologo(psicologa.getId()).stream().map(this::aHorario).toList());
    }

    @Transactional
    public MisHorariosVista agregarHorario(String correo, int dia, String inicioTexto, String finTexto, int duracion) {
        Usuario psicologa = usuario(correo);
        LocalTime inicio = LocalTime.parse(inicioTexto);
        LocalTime fin = LocalTime.parse(finTexto);

        if (!fin.isAfter(inicio)) {
            throw new ReglaNegocioException("La hora de fin debe ser posterior a la hora de inicio");
        }
        if (Duration.between(inicio, fin).toMinutes() < duracion) {
            throw new ReglaNegocioException("El bloque es más corto que la duración de una cita");
        }
        for (HorarioAtencion existente : horarioRepository.listarPorPsicologo(psicologa.getId())) {
            if (existente.getDiaSemana() == dia
                    && inicio.isBefore(existente.getHoraFin()) && fin.isAfter(existente.getHoraInicio())) {
                throw new ReglaNegocioException("Ese horario se cruza con otro que ya tienes ese día");
            }
        }

        HorarioAtencion horario = new HorarioAtencion();
        horario.setPsicologo(psicologa);
        horario.setDiaSemana(dia);
        horario.setHoraInicio(inicio);
        horario.setHoraFin(fin);
        horario.setDuracionMinutos(duracion);
        horarioRepository.guardar(horario);
        return misHorarios(correo);
    }

    @Transactional
    public MisHorariosVista quitarHorario(String correo, Integer id) {
        Usuario psicologa = usuario(correo);
        HorarioAtencion horario = horarioRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Horario no encontrado"));
        if (!horario.getPsicologo().getId().equals(psicologa.getId())) {
            throw new AccesoDenegadoException("Ese horario no te pertenece");
        }
        horario.setEliminado(true);
        horarioRepository.guardar(horario);
        return misHorarios(correo);
    }

    @Transactional
    public MisHorariosVista guardarTarifa(String correo, BigDecimal tarifa) {
        Usuario psicologa = usuario(correo);
        if (tarifa.compareTo(BigDecimal.ZERO) < 0 || tarifa.compareTo(new BigDecimal("9999.99")) > 0) {
            throw new ReglaNegocioException("La tarifa debe estar entre 0 y 9999.99");
        }
        psicologa.setTarifaConsulta(tarifa.setScale(2, RoundingMode.HALF_UP));
        usuarioRepository.guardar(psicologa);
        return misHorarios(correo);
    }

    // ---------- Espacios libres ----------

    @Transactional(readOnly = true)
    public List<EspacioVista> espaciosLibres(Integer pacienteId, String correo, String rol) {
        Paciente paciente = pacientes.obtenerParaLectura(pacienteId, correo, rol);
        if (paciente.getPsicologo() == null) {
            throw new ReglaNegocioException("Este niño o niña no tiene psicóloga asignada");
        }
        return calcularEspacios(paciente.getPsicologo());
    }

    private List<EspacioVista> calcularEspacios(Usuario psicologa) {
        LocalDate hoy = LocalDate.now();
        LocalDateTime limite = LocalDateTime.now().plusMinutes(30);
        List<HorarioAtencion> bloques = horarioRepository.listarPorPsicologo(psicologa.getId());

        Set<String> ocupados = citaRepository
                .listarEntre(psicologa.getId(), hoy, hoy.plusDays(DIAS_VISIBLES), ESTADOS_ACTIVOS).stream()
                .map(c -> c.getFecha() + "T" + c.getHoraInicio())
                .collect(Collectors.toSet());

        List<EspacioVista> libres = new ArrayList<>();
        for (int i = 0; i < DIAS_VISIBLES; i++) {
            LocalDate fecha = hoy.plusDays(i);
            for (HorarioAtencion bloque : bloques) {
                if (bloque.getDiaSemana() != fecha.getDayOfWeek().getValue()) {
                    continue;
                }
                LocalTime inicio = bloque.getHoraInicio();
                while (Duration.between(inicio, bloque.getHoraFin()).toMinutes() >= bloque.getDuracionMinutos()) {
                    LocalTime fin = inicio.plusMinutes(bloque.getDuracionMinutos());
                    boolean yaPaso = LocalDateTime.of(fecha, inicio).isBefore(limite);
                    if (!yaPaso && !ocupados.contains(fecha + "T" + inicio)) {
                        libres.add(new EspacioVista(fecha.toString(), HORA.format(inicio), HORA.format(fin)));
                    }
                    inicio = fin;
                }
            }
        }
        libres.sort(Comparator.comparing(EspacioVista::fecha).thenComparing(EspacioVista::horaInicio));
        return libres;
    }

    // ---------- Citas ----------

    @Transactional(readOnly = true)
    public List<CitaVista> listar(String correo, String rol) {
        List<Cita> citas;
        if ("ADMIN".equals(rol)) {
            citas = citaRepository.listarTodas();
        } else if ("PSICOLOGO".equals(rol)) {
            citas = citaRepository.listarPorPsicologo(correo);
        } else {
            List<Integer> ids = pacienteRepository.listarPorTutor(correo).stream().map(Paciente::getId).toList();
            citas = ids.isEmpty() ? List.of() : citaRepository.listarPorPacientes(ids);
        }
        return citas.stream()
                .sorted(Comparator.comparing(Cita::getFecha).thenComparing(Cita::getHoraInicio))
                .map(c -> aVista(c, rol))
                .toList();
    }

    @Transactional
    public CitaVista crear(Integer pacienteId, LocalDate fecha, String horaTexto, String motivo, boolean urgente,
                           String correo, String rol) {
        boolean esTutor = "TUTOR".equals(rol);
        if (!esTutor && !"PSICOLOGO".equals(rol)) {
            throw new AccesoDenegadoException("Solo el tutor o la psicóloga pueden pedir o proponer una cita");
        }
        Paciente paciente = pacientes.obtenerParaLectura(pacienteId, correo, rol);
        Usuario psicologa = paciente.getPsicologo();
        if (psicologa == null) {
            throw new ReglaNegocioException("Este niño o niña no tiene psicóloga asignada");
        }
        Usuario solicitante = usuario(correo);

        String hora = HORA.format(LocalTime.parse(horaTexto));
        EspacioVista espacio = calcularEspacios(psicologa).stream()
                .filter(e -> e.fecha().equals(fecha.toString()) && e.horaInicio().equals(hora))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException(
                        "Ese horario ya no está disponible. Elige otro espacio libre."));

        Cita cita = new Cita();
        cita.setPsicologo(psicologa);
        cita.setPaciente(paciente);
        cita.setSolicitante(solicitante);
        cita.setFecha(fecha);
        cita.setHoraInicio(LocalTime.parse(espacio.horaInicio()));
        cita.setHoraFin(LocalTime.parse(espacio.horaFin()));
        cita.setMotivo(motivo.trim());
        cita.setUrgente(esTutor && urgente);
        cita.setEstado(esTutor ? SOLICITADA : PROPUESTA);
        cita.setCosto(psicologa.getTarifaConsulta());

        try {
            return aVista(citaRepository.guardar(cita), rol);
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException("Ese horario acaba de ser tomado por otra persona. Elige otro.");
        }
    }

    @Transactional
    public CitaVista accion(Integer id, String accion, String comentario, BigDecimal costo, String correo, String rol) {
        Cita cita = cargarConAcceso(id, correo, rol);
        if (!acciones(cita, rol).contains(accion)) {
            throw new ReglaNegocioException("Esa acción no está disponible para esta cita");
        }

        switch (accion) {
            case "CONFIRMAR" -> cita.setEstado(CONFIRMADA);
            case "RECHAZAR" -> cita.setEstado(RECHAZADA);
            case "CANCELAR" -> cita.setEstado(CANCELADA);
            case "ATENDIDA" -> {
                cita.setEstado(ATENDIDA);
                if (costo != null) {
                    cita.setCosto(costo.setScale(2, RoundingMode.HALF_UP));
                }
            }
            case "NO_ASISTIO" -> cita.setEstado(NO_ASISTIO);
            default -> throw new ReglaNegocioException("Acción no válida");
        }
        if ("PSICOLOGO".equals(rol) && comentario != null && !comentario.isBlank()) {
            cita.setComentarioPsicologa(comentario.trim());
        }
        return aVista(citaRepository.guardar(cita), rol);
    }

    // ---------- Apoyo ----------

    private Cita cargarConAcceso(Integer id, String correo, String rol) {
        Cita cita = citaRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));
        boolean permitido = "ADMIN".equals(rol)
                || ("PSICOLOGO".equals(rol) && cita.getPsicologo().getCorreo().equals(correo))
                || ("TUTOR".equals(rol) && cita.getPaciente().esTutor(correo));
        if (!permitido) {
            throw new AccesoDenegadoException("No tienes acceso a esta cita");
        }
        return cita;
    }

    // Qué botones le corresponden a cada persona según el estado de la cita
    private List<String> acciones(Cita cita, String rol) {
        boolean psicologa = "PSICOLOGO".equals(rol);
        boolean tutor = "TUTOR".equals(rol);
        List<String> lista = new ArrayList<>();
        switch (cita.getEstado()) {
            case SOLICITADA -> {
                if (psicologa) {
                    lista.add("CONFIRMAR");
                    lista.add("RECHAZAR");
                }
                if (tutor) {
                    lista.add("CANCELAR");
                }
            }
            case PROPUESTA -> {
                if (tutor) {
                    lista.add("CONFIRMAR");
                    lista.add("RECHAZAR");
                }
                if (psicologa) {
                    lista.add("CANCELAR");
                }
            }
            case CONFIRMADA -> {
                if (psicologa || tutor) {
                    lista.add("CANCELAR");
                }
                if (psicologa && !cita.getFecha().isAfter(LocalDate.now())) {
                    lista.add("ATENDIDA");
                    lista.add("NO_ASISTIO");
                }
            }
            default -> { }
        }
        return lista;
    }

    private Usuario usuario(String correo) {
        return usuarioRepository.buscarPorCorreo(correo)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado"));
    }

    private HorarioVista aHorario(HorarioAtencion h) {
        return new HorarioVista(h.getId(), h.getDiaSemana(), HORA.format(h.getHoraInicio()),
                HORA.format(h.getHoraFin()), h.getDuracionMinutos());
    }

    private CitaVista aVista(Cita c, String rol) {
        Usuario solicitante = c.getSolicitante();
        return new CitaVista(c.getId(), c.getPaciente().getId(), c.getPaciente().getNombreCompleto(),
                c.getPsicologo().getNombreCompleto(), solicitante.getNombreCompleto(),
                solicitante.getRol().getNombreRol(), c.getFecha().toString(), HORA.format(c.getHoraInicio()),
                HORA.format(c.getHoraFin()), c.getMotivo(), Boolean.TRUE.equals(c.getUrgente()), c.getEstado(),
                c.getCosto(), c.getComentarioPsicologa(), acciones(c, rol));
    }
}