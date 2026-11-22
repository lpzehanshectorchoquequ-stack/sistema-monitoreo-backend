package com.integrador.sistema_monitoreo_backend.application;

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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class GestionarEvaluacionesUseCase {

    public record PreguntaVista(Integer id, int orden, String texto, String subescala) {}

    public record InstrumentoVista(Integer id, String codigo, String nombre, String descripcion,
                                   String trastorno, String quienResponde, String estadoValidacion,
                                   int totalPreguntas, List<PreguntaVista> preguntas) {}

    public record ConclusionVista(String trastorno, String nivel, String recomendacion,
                                  List<String> explicacion) {}

    public record EvaluacionVista(Integer id, Integer pacienteId, String pacienteNombre,
                                  String instrumentoNombre, String estadoValidacion,
                                  LocalDate fecha, String observaciones, ConclusionVista conclusion) {}

    private final InstrumentoRepository instrumentoRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final GestionarPacientesUseCase pacientes;
    private final MotorInferencia motor = new MotorInferencia();

    public GestionarEvaluacionesUseCase(InstrumentoRepository instrumentoRepository,
                                        EvaluacionRepository evaluacionRepository,
                                        UsuarioRepository usuarioRepository,
                                        GestionarPacientesUseCase pacientes) {
        this.instrumentoRepository = instrumentoRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.pacientes = pacientes;
    }

    @Transactional(readOnly = true)
    public List<InstrumentoVista> listarInstrumentos() {
        return instrumentoRepository.listarTodos().stream().map(i -> aVista(i, false)).toList();
    }

    @Transactional(readOnly = true)
    public InstrumentoVista obtenerInstrumento(Integer id) {
        Instrumento instrumento = instrumentoRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Instrumento no encontrado"));
        return aVista(instrumento, true);
    }

    @Transactional
    public EvaluacionVista aplicar(Integer pacienteId, Integer instrumentoId,
                                   Map<Integer, Integer> respuestas, String observaciones,
                                   String correo, String rol) {
        Paciente paciente = pacientes.obtener(pacienteId, correo, rol);
        Instrumento instrumento = instrumentoRepository.buscarPorId(instrumentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Instrumento no encontrado"));
        Usuario psicologo = usuarioRepository.buscarPorCorreo(correo)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado"));

        validarRespuestas(instrumento, respuestas);
        MotorInferencia.Resultado resultado = motor.evaluar(instrumento, respuestas);

        Evaluacion evaluacion = new Evaluacion();
        evaluacion.setPaciente(paciente);
        evaluacion.setPsicologo(psicologo);
        evaluacion.setInstrumento(instrumento);
        evaluacion.setFechaEvaluacion(LocalDate.now());
        evaluacion.setObservaciones(observaciones);

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

        return aVista(evaluacionRepository.guardar(evaluacion));
    }

    @Transactional(readOnly = true)
    public List<EvaluacionVista> historial(Integer pacienteId, String correo, String rol) {
        pacientes.obtener(pacienteId, correo, rol); // valida que tenga acceso a este paciente
        return evaluacionRepository.listarPorPaciente(pacienteId).stream()
                .filter(e -> "APROBADA".equals(e.getEstado()))
                .map(this::aVista)
                .toList();
    }

    private void validarRespuestas(Instrumento instrumento, Map<Integer, Integer> respuestas) {
        for (Pregunta pregunta : instrumento.getPreguntas()) {
            Integer valor = respuestas.get(pregunta.getId());
            if (valor == null) {
                throw new ReglaNegocioException("Falta responder la pregunta " + pregunta.getOrden());
            }
            if (valor < 0 || valor > 3) {
                throw new ReglaNegocioException("Respuesta fuera de rango en la pregunta " + pregunta.getOrden());
            }
        }
    }

    private InstrumentoVista aVista(Instrumento i, boolean conPreguntas) {
        List<PreguntaVista> preguntas = conPreguntas
                ? i.getPreguntas().stream()
                    .map(p -> new PreguntaVista(p.getId(), p.getOrden(), p.getTexto(), p.getSubescala()))
                    .toList()
                : List.of();
        return new InstrumentoVista(i.getId(), i.getCodigo(), i.getNombre(), i.getDescripcion(),
                i.getTrastorno(), i.getQuienResponde(), i.getEstadoValidacion(),
                i.getPreguntas().size(), preguntas);
    }

    private EvaluacionVista aVista(Evaluacion e) {
        ConclusionVista conclusion = null;
        if (!e.getConclusiones().isEmpty()) {
            Conclusion c = e.getConclusiones().get(0);
            conclusion = new ConclusionVista(c.getTrastorno(), c.getNivel(), c.getRecomendacion(),
                    Arrays.asList(c.getExplicacion().split("\n")));
        }
        return new EvaluacionVista(e.getId(), e.getPaciente().getId(), e.getPaciente().getNombre(),
                e.getInstrumento().getNombre(), e.getInstrumento().getEstadoValidacion(),
                e.getFechaEvaluacion(), e.getObservaciones(), conclusion);
    }
}