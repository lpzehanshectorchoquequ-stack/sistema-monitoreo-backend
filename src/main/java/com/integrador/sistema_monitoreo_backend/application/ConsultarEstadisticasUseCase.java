package com.integrador.sistema_monitoreo_backend.application;

import com.integrador.sistema_monitoreo_backend.domain.Evaluacion;
import com.integrador.sistema_monitoreo_backend.domain.EvaluacionRepository;
import com.integrador.sistema_monitoreo_backend.domain.MotorInferencia;
import com.integrador.sistema_monitoreo_backend.domain.Paciente;
import com.integrador.sistema_monitoreo_backend.domain.PacienteRepository;
import com.integrador.sistema_monitoreo_backend.domain.Respuesta;
import com.integrador.sistema_monitoreo_backend.domain.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ConsultarEstadisticasUseCase {

    public record PuntoEvolucion(Integer evaluacionId, LocalDate fecha, String instrumento, String trastorno,
                                 String nivel, int puntaje, int puntajeMaximo, int porcentaje,
                                 int sintomas, int totalPreguntas) {}

    public record EvolucionVista(Integer pacienteId, String pacienteNombre, List<PuntoEvolucion> puntos) {}

    public record Conteo(String etiqueta, long valor) {}

    public record ResumenVista(long totalPacientes, long totalTutores, long totalEvaluaciones,
                               List<Conteo> porNivel, List<Conteo> porTrastorno, List<Conteo> porMes,
                               List<Conteo> porEdad, List<Conteo> porGenero, List<Conteo> porPsicologa) {}

    private final EvaluacionRepository evaluacionRepository;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final GestionarPacientesUseCase pacientes;

    public ConsultarEstadisticasUseCase(EvaluacionRepository evaluacionRepository,
                                        PacienteRepository pacienteRepository,
                                        UsuarioRepository usuarioRepository,
                                        GestionarPacientesUseCase pacientes) {
        this.evaluacionRepository = evaluacionRepository;
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.pacientes = pacientes;
    }

    @Transactional(readOnly = true)
    public EvolucionVista evolucion(Integer pacienteId, String correo, String rol) {
        Paciente paciente = pacientes.obtenerParaLectura(pacienteId, correo, rol);

        // El repositorio entrega de la más nueva a la más antigua; la gráfica necesita lo contrario
        List<Evaluacion> ordenadas = new ArrayList<>(evaluacionRepository.listarPorPaciente(pacienteId));
        Collections.reverse(ordenadas);

        List<PuntoEvolucion> puntos = new ArrayList<>();
        for (Evaluacion e : ordenadas) {
            int puntaje = 0;
            int sintomas = 0;
            for (Respuesta r : e.getRespuestas()) {
                puntaje += r.getValor();
                if (r.getValor() >= MotorInferencia.UMBRAL_SINTOMA) {
                    sintomas++;
                }
            }
            int total = e.getRespuestas().size();
            int maximo = total * 3;
            int porcentaje = maximo == 0 ? 0 : Math.round(puntaje * 100f / maximo);
            puntos.add(new PuntoEvolucion(e.getId(), e.getFechaEvaluacion(), e.getInstrumento().getNombre(),
                    e.getInstrumento().getTrastorno(), nivelDe(e), puntaje, maximo, porcentaje, sintomas, total));
        }
        return new EvolucionVista(paciente.getId(), paciente.getNombre(), puntos);
    }

    @Transactional(readOnly = true)
    public ResumenVista resumen(String correo, String rol) {
        boolean esAdmin = "ADMIN".equals(rol);
        List<Paciente> misPacientes = esAdmin
                ? pacienteRepository.listarTodos()
                : pacienteRepository.listarPorPsicologo(correo);
        Set<Integer> ids = misPacientes.stream().map(Paciente::getId).collect(Collectors.toSet());
        List<Evaluacion> evaluaciones = evaluacionRepository.listarTodas().stream()
                .filter(e -> ids.contains(e.getPaciente().getId()))
                .toList();
        long tutores = esAdmin
                ? usuarioRepository.listarPorRol("Tutor").size()
                : usuarioRepository.listarPorPsicologo(correo).size();

        return new ResumenVista(
                misPacientes.size(),
                tutores,
                evaluaciones.size(),
                contarOrdenado(evaluaciones.stream().map(this::nivelDe),
                        List.of("SIN_INDICADORES", "MODERADO", "ALTO")),
                contar(evaluaciones.stream().map(e -> e.getInstrumento().getTrastorno())),
                porMes(evaluaciones),
                porEdad(misPacientes),
                contar(misPacientes.stream().map(p -> p.getGenero() == null ? "SIN_DATO" : p.getGenero())),
                esAdmin
                        ? contar(misPacientes.stream()
                            .map(p -> p.getPsicologo() == null ? "Sin asignar" : p.getPsicologo().getNombre()))
                        : List.of());
    }

    private String nivelDe(Evaluacion e) {
        return e.getConclusiones().isEmpty() ? "SIN_INDICADORES" : e.getConclusiones().get(0).getNivel();
    }

    private List<Conteo> contar(Stream<String> valores) {
        Map<String, Long> mapa = valores.collect(
                Collectors.groupingBy(v -> v, LinkedHashMap::new, Collectors.counting()));
        return mapa.entrySet().stream().map(en -> new Conteo(en.getKey(), en.getValue())).toList();
    }

    private List<Conteo> contarOrdenado(Stream<String> valores, List<String> orden) {
        Map<String, Long> mapa = valores.collect(Collectors.groupingBy(v -> v, Collectors.counting()));
        return orden.stream().map(clave -> new Conteo(clave, mapa.getOrDefault(clave, 0L))).toList();
    }

    private List<Conteo> porMes(List<Evaluacion> evaluaciones) {
        YearMonth actual = YearMonth.now();
        Map<YearMonth, Long> mapa = evaluaciones.stream().collect(
                Collectors.groupingBy(e -> YearMonth.from(e.getFechaEvaluacion()), Collectors.counting()));
        List<Conteo> resultado = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth mes = actual.minusMonths(i);
            resultado.add(new Conteo(mes.toString(), mapa.getOrDefault(mes, 0L)));
        }
        return resultado;
    }

    private List<Conteo> porEdad(List<Paciente> lista) {
        long primero = 0;
        long segundo = 0;
        long tercero = 0;
        for (Paciente p : lista) {
            int edad = Period.between(p.getFechaNacimiento(), LocalDate.now()).getYears();
            if (edad <= 7) {
                primero++;
            } else if (edad <= 9) {
                segundo++;
            } else {
                tercero++;
            }
        }
        return List.of(new Conteo("6-7 años", primero), new Conteo("8-9 años", segundo),
                new Conteo("10-12 años", tercero));
    }
}