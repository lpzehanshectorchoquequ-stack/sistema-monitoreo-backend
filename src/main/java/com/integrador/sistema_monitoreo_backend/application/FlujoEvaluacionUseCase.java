package com.integrador.sistema_monitoreo_backend.application;

import com.integrador.sistema_monitoreo_backend.application.GestionarEvaluacionesUseCase.ConclusionVista;
import com.integrador.sistema_monitoreo_backend.application.GestionarEvaluacionesUseCase.PreguntaVista;
import com.integrador.sistema_monitoreo_backend.domain.AccesoDenegadoException;
import com.integrador.sistema_monitoreo_backend.domain.Conclusion;
import com.integrador.sistema_monitoreo_backend.domain.Evaluacion;
import com.integrador.sistema_monitoreo_backend.domain.EvaluacionRepository;
import com.integrador.sistema_monitoreo_backend.domain.Instrumento;
import com.integrador.sistema_monitoreo_backend.domain.InstrumentoRepository;
import com.integrador.sistema_monitoreo_backend.domain.MotorInferencia;
import com.integrador.sistema_monitoreo_backend.domain.Paciente;
import com.integrador.sistema_monitoreo_backend.domain.Pregunta;
import com.integrador.sistema_monitoreo_backend.domain.RecursoNoEncontradoException;
import com.integrador.sistema_monitoreo_backend.domain.ReglaNegocioException;
import com.integrador.sistema_monitoreo_backend.domain.Respuesta;
import com.integrador.sistema_monitoreo_backend.domain.Usuario;
import com.integrador.sistema_monitoreo_backend.domain.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FlujoEvaluacionUseCase {

    public record AreaVista(String nombre, int sintomas, int totalPreguntas, int puntaje,
                            int puntajeMaximo, int porcentaje) {}

    public record RespuestaVista(Integer preguntaId, int orden, String texto, String subescala, int valor) {}

    public record ResumenEvaluacion(Integer id, Integer pacienteId, String pacienteNombre, String instrumentoNombre,
                                    String trastorno, String estado, LocalDate fecha, String nivel) {}

    public record DetalleEvaluacion(Integer id, Integer pacienteId, String pacienteNombre, String instrumentoNombre,
                                    String trastorno, String descripcion, String estado, LocalDate fecha,
                                    String observaciones, String comentarioPsicologa, String respondidoPor,
                                    String aprobadoPor, int cambiosRespuestas, boolean puedeResponder,
                                    boolean puedeAprobar, boolean puedeVerResultado,
                                    List<PreguntaVista> preguntas, List<RespuestaVista> respuestas,
                                    ConclusionVista conclusion, List<AreaVista> areas) {}

    private static final String PENDIENTE = "PENDIENTE";
    private static final String POR_REVISAR = "POR_REVISAR";
    private static final String APROBADA = "APROBADA";

    private final EvaluacionRepository evaluacionRepository;
    private final InstrumentoRepository instrumentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final GestionarPacientesUseCase pacientes;
    private final MotorInferencia motor = new MotorInferencia();

    public FlujoEvaluacionUseCase(EvaluacionRepository evaluacionRepository,
                                  InstrumentoRepository instrumentoRepository,
                                  UsuarioRepository usuarioRepository,
                                  GestionarPacientesUseCase pacientes) {
        this.evaluacionRepository = evaluacionRepository;
        this.instrumentoRepository = instrumentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pacientes = pacientes;
    }

    // La psicóloga envía un test al tutor
    @Transactional
    public DetalleEvaluacion asignar(Integer pacienteId, Integer instrumentoId, String correo, String rol) {
        Paciente paciente = pacientes.obtener(pacienteId, correo, rol);
        Instrumento instrumento = instrumentoRepository.buscarPorId(instrumentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Test no encontrado"));

        if (!"Tutor".equals(instrumento.getQuienResponde())) {
            throw new ReglaNegocioException(
                    "Este test no lo responde el tutor. Aplícalo tú con la opción 'Responder yo ahora'");
        }
        if (instrumento.getPreguntas().isEmpty()) {
            throw new ReglaNegocioException("Este test aún no tiene preguntas");
        }
        boolean yaPendiente = evaluacionRepository.listarPorPaciente(pacienteId).stream()
                .anyMatch(e -> PENDIENTE.equals(e.getEstado()) && e.getInstrumento().getId().equals(instrumentoId));
        if (yaPendiente) {
            throw new ReglaNegocioException("Ya hay una evaluación pendiente de este test para este niño o niña");
        }
        Usuario psicologa = usuarioRepository.buscarPorCorreo(correo)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado"));

        Evaluacion evaluacion = new Evaluacion();
        evaluacion.setPaciente(paciente);
        evaluacion.setPsicologo(psicologa);
        evaluacion.setInstrumento(instrumento);
        evaluacion.setFechaEvaluacion(LocalDate.now());
        evaluacion.setEstado(PENDIENTE);
        return aDetalle(evaluacionRepository.guardar(evaluacion), rol);
    }

    @Transactional(readOnly = true)
    public List<ResumenEvaluacion> listar(String correo, String rol) {
        return evaluacionRepository.listarTodas().stream()
                .filter(e -> visiblePara(e, correo, rol))
                .sorted(Comparator.comparing(Evaluacion::getFechaEvaluacion).reversed()
                        .thenComparing(Evaluacion::getId, Comparator.reverseOrder()))
                .map(e -> aResumen(e, rol))
                .toList();
    }

    @Transactional(readOnly = true)
    public DetalleEvaluacion detalle(Integer id, String correo, String rol) {
        return aDetalle(cargarConAcceso(id, correo, rol), rol);
    }

    @Transactional
    public DetalleEvaluacion responder(Integer id, Map<Integer, Integer> respuestas, int cambios,
                                       String observaciones, String correo, String rol) {
        Evaluacion evaluacion = cargarConAcceso(id, correo, rol);
        boolean esTutor = "TUTOR".equals(rol);
        if (!esTutor && !"PSICOLOGO".equals(rol)) {
            throw new AccesoDenegadoException("Solo el tutor o la psicóloga pueden responder este test");
        }
        if (!PENDIENTE.equals(evaluacion.getEstado())) {
            throw new ReglaNegocioException("Esta evaluación ya fue respondida");
        }
        if (cambios < 0 || cambios > 2) {
            throw new ReglaNegocioException("Solo se permite cambiar hasta 2 respuestas");
        }

        Instrumento instrumento = evaluacion.getInstrumento();
        for (Pregunta pregunta : instrumento.getPreguntas()) {
            Integer valor = respuestas.get(pregunta.getId());
            if (valor == null) {
                throw new ReglaNegocioException("Falta responder la pregunta " + pregunta.getOrden());
            }
            if (valor < 0 || valor > 3) {
                throw new ReglaNegocioException("Respuesta fuera de rango en la pregunta " + pregunta.getOrden());
            }
        }
        Usuario quien = usuarioRepository.buscarPorCorreo(correo)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado"));

        MotorInferencia.Resultado resultado = motor.evaluar(instrumento, respuestas);

        for (Pregunta pregunta : instrumento.getPreguntas()) {
            Respuesta respuesta = new Respuesta();
            respuesta.setEvaluacion(evaluacion);
            respuesta.setPregunta(pregunta);
            respuesta.setValor(respuestas.get(pregunta.getId()));
            evaluacion.getRespuestas().add(respuesta);
        }
        Conclusion conclusion = new Conclusion();
        conclusion.setEvaluacion(evaluacion);
        conclusion.setTrastorno(resultado.trastorno());
        conclusion.setNivel(resultado.nivel());
        conclusion.setExplicacion(String.join("\n", resultado.explicacion()));
        conclusion.setRecomendacion(resultado.recomendacion());
        evaluacion.getConclusiones().add(conclusion);

        LocalDateTime ahora = LocalDateTime.now();
        evaluacion.setObservaciones(observaciones);
        evaluacion.setCambiosRespuestas(cambios);
        evaluacion.setRespondidoPor(quien);
        evaluacion.setFechaRespuesta(ahora);
        evaluacion.setFechaEvaluacion(LocalDate.now());
        if (esTutor) {
            evaluacion.setEstado(POR_REVISAR);
        } else {
            // Lo que responde la propia psicóloga no necesita otra aprobación
            evaluacion.setEstado(APROBADA);
            evaluacion.setAprobadoPor(quien);
            evaluacion.setFechaAprobacion(ahora);
        }
        return aDetalle(evaluacionRepository.guardar(evaluacion), rol);
    }

    @Transactional
    public DetalleEvaluacion aprobar(Integer id, String comentario, String correo, String rol) {
        Evaluacion evaluacion = cargarConAcceso(id, correo, rol);
        if (!"PSICOLOGO".equals(rol)) {
            throw new AccesoDenegadoException("Solo la psicóloga puede aprobar un resultado");
        }
        if (!POR_REVISAR.equals(evaluacion.getEstado())) {
            throw new ReglaNegocioException("Esta evaluación no está esperando aprobación");
        }
        Usuario psicologa = usuarioRepository.buscarPorCorreo(correo)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado"));

        evaluacion.setEstado(APROBADA);
        evaluacion.setAprobadoPor(psicologa);
        evaluacion.setFechaAprobacion(LocalDateTime.now());
        evaluacion.setComentarioPsicologa(comentario == null || comentario.isBlank() ? null : comentario.trim());
        return aDetalle(evaluacionRepository.guardar(evaluacion), rol);
    }

    @Transactional
    public void cancelar(Integer id, String correo, String rol) {
        Evaluacion evaluacion = cargarConAcceso(id, correo, rol);
        if (!"PSICOLOGO".equals(rol)) {
            throw new AccesoDenegadoException("Solo la psicóloga puede cancelar una asignación");
        }
        if (!PENDIENTE.equals(evaluacion.getEstado())) {
            throw new ReglaNegocioException("Solo se puede cancelar una evaluación que aún no fue respondida");
        }
        evaluacion.setEliminado(true);
        evaluacionRepository.guardar(evaluacion);
    }

    // ---------- Apoyo ----------

    private Evaluacion cargarConAcceso(Integer id, String correo, String rol) {
        Evaluacion evaluacion = evaluacionRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evaluación no encontrada"));
        if (!visiblePara(evaluacion, correo, rol)) {
            throw new AccesoDenegadoException("No tienes acceso a esta evaluación");
        }
        return evaluacion;
    }

    private boolean visiblePara(Evaluacion e, String correo, String rol) {
        Paciente p = e.getPaciente();
        if ("ADMIN".equals(rol)) {
            return true;
        }
        if ("PSICOLOGO".equals(rol)) {
            return p.getPsicologo() != null && p.getPsicologo().getCorreo().equals(correo);
        }
        if ("TUTOR".equals(rol)) {
            return p.getTutor() != null && p.getTutor().getCorreo().equals(correo);
        }
        return false;
    }

    private ResumenEvaluacion aResumen(Evaluacion e, String rol) {
        boolean verNivel = APROBADA.equals(e.getEstado())
                || (POR_REVISAR.equals(e.getEstado()) && !"TUTOR".equals(rol));
        String nivel = verNivel && !e.getConclusiones().isEmpty() ? e.getConclusiones().get(0).getNivel() : null;
        return new ResumenEvaluacion(e.getId(), e.getPaciente().getId(), e.getPaciente().getNombre(),
                e.getInstrumento().getNombre(), e.getInstrumento().getTrastorno(), e.getEstado(),
                e.getFechaEvaluacion(), nivel);
    }

    private DetalleEvaluacion aDetalle(Evaluacion e, String rol) {
        boolean esTutor = "TUTOR".equals(rol);
        boolean puedeResponder = PENDIENTE.equals(e.getEstado()) && (esTutor || "PSICOLOGO".equals(rol));
        boolean puedeAprobar = POR_REVISAR.equals(e.getEstado()) && "PSICOLOGO".equals(rol);
        boolean verResultado = APROBADA.equals(e.getEstado())
                || (POR_REVISAR.equals(e.getEstado()) && !esTutor);

        List<PreguntaVista> preguntas = puedeResponder
                ? e.getInstrumento().getPreguntas().stream()
                    .map(p -> new PreguntaVista(p.getId(), p.getOrden(), p.getTexto(), p.getSubescala()))
                    .toList()
                : List.of();

        List<RespuestaVista> respuestas = verResultado
                ? respuestasOrdenadas(e).stream()
                    .map(r -> new RespuestaVista(r.getPregunta().getId(), r.getPregunta().getOrden(),
                            r.getPregunta().getTexto(), r.getPregunta().getSubescala(), r.getValor()))
                    .toList()
                : List.of();

        ConclusionVista conclusion = null;
        if (verResultado && !e.getConclusiones().isEmpty()) {
            Conclusion c = e.getConclusiones().get(0);
            conclusion = new ConclusionVista(c.getTrastorno(), c.getNivel(), c.getRecomendacion(),
                    Arrays.asList(c.getExplicacion().split("\n")));
        }

        return new DetalleEvaluacion(e.getId(), e.getPaciente().getId(), e.getPaciente().getNombre(),
                e.getInstrumento().getNombre(), e.getInstrumento().getTrastorno(), e.getInstrumento().getDescripcion(),
                e.getEstado(), e.getFechaEvaluacion(), e.getObservaciones(), e.getComentarioPsicologa(),
                e.getRespondidoPor() == null ? null : e.getRespondidoPor().getNombre(),
                e.getAprobadoPor() == null ? null : e.getAprobadoPor().getNombre(),
                e.getCambiosRespuestas(), puedeResponder, puedeAprobar, verResultado,
                preguntas, respuestas, conclusion, verResultado ? calcularAreas(e) : List.of());
    }

    // Se ignoran las respuestas de preguntas que ya se quitaron del test
    private List<Respuesta> respuestasOrdenadas(Evaluacion e) {
        return e.getRespuestas().stream()
                .filter(r -> r.getPregunta() != null)
                .sorted(Comparator.comparing(r -> r.getPregunta().getOrden()))
                .toList();
    }

    private List<AreaVista> calcularAreas(Evaluacion e) {
        Map<String, int[]> acumulado = new LinkedHashMap<>(); // [síntomas, total, puntaje]
        for (Respuesta r : respuestasOrdenadas(e)) {
            int[] datos = acumulado.computeIfAbsent(r.getPregunta().getSubescala(), k -> new int[3]);
            datos[0] += r.getValor() >= MotorInferencia.UMBRAL_SINTOMA ? 1 : 0;
            datos[1] += 1;
            datos[2] += r.getValor();
        }
        List<AreaVista> areas = new ArrayList<>();
        acumulado.forEach((nombre, d) -> {
            int maximo = d[1] * 3;
            int porcentaje = maximo == 0 ? 0 : Math.round(d[2] * 100f / maximo);
            areas.add(new AreaVista(nombre, d[0], d[1], d[2], maximo, porcentaje));
        });
        return areas;
    }
}
