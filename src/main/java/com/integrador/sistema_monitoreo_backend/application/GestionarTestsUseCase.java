package com.integrador.sistema_monitoreo_backend.application;

import com.integrador.sistema_monitoreo_backend.domain.CondicionRegla;
import com.integrador.sistema_monitoreo_backend.domain.EvaluacionRepository;
import com.integrador.sistema_monitoreo_backend.domain.Instrumento;
import com.integrador.sistema_monitoreo_backend.domain.InstrumentoRepository;
import com.integrador.sistema_monitoreo_backend.domain.Pregunta;
import com.integrador.sistema_monitoreo_backend.domain.RecursoNoEncontradoException;
import com.integrador.sistema_monitoreo_backend.domain.Regla;
import com.integrador.sistema_monitoreo_backend.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class GestionarTestsUseCase {

    public record PreguntaTest(Integer id, int orden, String texto, String subescala) {}

    public record CondicionTest(String hecho, String operador, double valor) {}

    public record ReglaTest(String codigo, String descripcion, String nivel, String hechoDerivado,
                            List<CondicionTest> condiciones) {}

    public record TestResumen(Integer id, String nombre, String trastorno, String quienResponde,
                              String estadoValidacion, int totalPreguntas, boolean usado) {}

    public record TestDetalle(Integer id, String codigo, String nombre, String descripcion, String trastorno,
                              String quienResponde, String estadoValidacion, boolean usado,
                              boolean umbralesEditables, Integer umbralModerado, Integer umbralAlto,
                              String advertencia, List<PreguntaTest> preguntas, List<ReglaTest> reglas) {}

    public record DatosTest(String nombre, String descripcion, String trastorno, String quienResponde,
                            String estadoValidacion) {}

    public record DatosPregunta(String texto, String subescala) {}

    private static final String COD_MODERADO = "R_AUTO_MODERADO";
    private static final String COD_ALTO = "R_AUTO_ALTO";
    private static final String REC_ALTO =
            "Se sugiere derivar a una evaluación clínica completa con la psicóloga. "
            + "Este resultado es un indicador de riesgo y no constituye un diagnóstico.";
    private static final String REC_MODERADO =
            "Se sugiere observar y dar seguimiento en distintos contextos, y repetir la evaluación. "
            + "Valorar con la psicóloga si corresponde profundizar.";

    private final InstrumentoRepository instrumentoRepository;
    private final EvaluacionRepository evaluacionRepository;

    public GestionarTestsUseCase(InstrumentoRepository instrumentoRepository,
                                 EvaluacionRepository evaluacionRepository) {
        this.instrumentoRepository = instrumentoRepository;
        this.evaluacionRepository = evaluacionRepository;
    }

    @Transactional(readOnly = true)
    public List<TestResumen> listar() {
        return instrumentoRepository.listarTodos().stream()
                .map(i -> new TestResumen(i.getId(), i.getNombre(), i.getTrastorno(), i.getQuienResponde(),
                        i.getEstadoValidacion(), activas(i).size(),
                        evaluacionRepository.existePorInstrumento(i.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TestDetalle obtener(Integer id) {
        return aDetalle(buscar(id));
    }

    @Transactional
    public TestDetalle crear(DatosTest datos) {
        Instrumento instrumento = new Instrumento();
        instrumento.setCodigo("TEST_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        aplicar(instrumento, datos);
        return aDetalle(instrumentoRepository.guardar(instrumento));
    }

    @Transactional
    public TestDetalle actualizar(Integer id, DatosTest datos) {
        Instrumento instrumento = buscar(id);
        aplicar(instrumento, datos);
        return aDetalle(instrumentoRepository.guardar(instrumento));
    }

    @Transactional
    public void eliminar(Integer id) {
        Instrumento instrumento = buscar(id);
        if (evaluacionRepository.existePorInstrumento(id)) {
            throw new ReglaNegocioException(
                    "Este test ya se usó en evaluaciones y no se puede eliminar. Puedes dejarlo como borrador.");
        }
        instrumento.setEliminado(true);
        instrumentoRepository.guardar(instrumento);
    }

    @Transactional
    public TestDetalle agregarPregunta(Integer id, DatosPregunta datos) {
        Instrumento instrumento = buscar(id);
        int siguiente = activas(instrumento).stream().mapToInt(Pregunta::getOrden).max().orElse(0) + 1;

        Pregunta pregunta = new Pregunta();
        pregunta.setInstrumento(instrumento);
        pregunta.setOrden(siguiente);
        pregunta.setTexto(datos.texto().trim());
        pregunta.setSubescala(codigoSubescala(datos.subescala()));
        instrumento.getPreguntas().add(pregunta);
        return aDetalle(instrumentoRepository.guardar(instrumento));
    }

    @Transactional
    public TestDetalle editarPregunta(Integer id, Integer preguntaId, DatosPregunta datos) {
        Instrumento instrumento = buscar(id);
        Pregunta pregunta = buscarPregunta(instrumento, preguntaId);
        pregunta.setTexto(datos.texto().trim());
        pregunta.setSubescala(codigoSubescala(datos.subescala()));
        return aDetalle(instrumentoRepository.guardar(instrumento));
    }

    @Transactional
    public TestDetalle quitarPregunta(Integer id, Integer preguntaId) {
        Instrumento instrumento = buscar(id);
        Pregunta pregunta = buscarPregunta(instrumento, preguntaId);
        if (activas(instrumento).size() <= 1) {
            throw new ReglaNegocioException("El test debe conservar al menos una pregunta");
        }
        pregunta.setEliminado(true);

        // Vuelve a numerar las que quedan: 1, 2, 3...
        int numero = 1;
        for (Pregunta p : activas(instrumento)) {
            p.setOrden(numero++);
        }
        return aDetalle(instrumentoRepository.guardar(instrumento));
    }

    @Transactional
    public TestDetalle guardarUmbrales(Integer id, int moderado, int alto) {
        Instrumento instrumento = buscar(id);
        List<Regla> reglas = instrumento.getReglas();

        if (reglas.stream().anyMatch(r -> !r.getCodigo().startsWith("R_AUTO_"))) {
            throw new ReglaNegocioException(
                    "Este test usa reglas avanzadas y no se pueden reemplazar por umbrales simples");
        }
        int total = activas(instrumento).size();
        if (moderado < 1 || alto <= moderado || alto > total) {
            throw new ReglaNegocioException("Los umbrales deben cumplir: 1 ≤ moderado < alto ≤ " + total
                    + " (la cantidad de preguntas del test)");
        }

        Regla reglaModerado = buscarRegla(reglas, COD_MODERADO);
        if (reglaModerado == null) {
            reglaModerado = nuevaRegla(instrumento, COD_MODERADO, "MODERADO", REC_MODERADO);
            agregarCondicion(reglaModerado, ">=", moderado);
            agregarCondicion(reglaModerado, "<", alto);
        } else {
            for (CondicionRegla c : reglaModerado.getCondiciones()) {
                c.setValor(BigDecimal.valueOf(">=".equals(c.getOperador()) ? moderado : alto));
            }
        }
        reglaModerado.setDescripcion("Se observan entre " + moderado + " y " + (alto - 1) + " síntomas");

        Regla reglaAlto = buscarRegla(reglas, COD_ALTO);
        if (reglaAlto == null) {
            reglaAlto = nuevaRegla(instrumento, COD_ALTO, "ALTO", REC_ALTO);
            agregarCondicion(reglaAlto, ">=", alto);
        } else {
            for (CondicionRegla c : reglaAlto.getCondiciones()) {
                c.setValor(BigDecimal.valueOf(alto));
            }
        }
        reglaAlto.setDescripcion("Se observan " + alto + " o más síntomas");

        return aDetalle(instrumentoRepository.guardar(instrumento));
    }

    // ---------- Apoyo ----------

    private Instrumento buscar(Integer id) {
        return instrumentoRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Test no encontrado"));
    }

    private Pregunta buscarPregunta(Instrumento instrumento, Integer preguntaId) {
        return activas(instrumento).stream()
                .filter(p -> Objects.equals(p.getId(), preguntaId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Pregunta no encontrada"));
    }

    private List<Pregunta> activas(Instrumento instrumento) {
        return instrumento.getPreguntas().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getEliminado()))
                .toList();
    }

    private void aplicar(Instrumento instrumento, DatosTest datos) {
        String nombre = datos.nombre().trim();
        boolean repetido = instrumentoRepository.listarTodos().stream()
                .anyMatch(otro -> !Objects.equals(otro.getId(), instrumento.getId())
                        && otro.getNombre().equalsIgnoreCase(nombre));
        if (repetido) {
            throw new ReglaNegocioException("Ya existe un test con ese nombre");
        }
        instrumento.setNombre(nombre);
        instrumento.setDescripcion(datos.descripcion() == null ? "" : datos.descripcion().trim());
        instrumento.setTrastorno(datos.trastorno());
        instrumento.setQuienResponde(datos.quienResponde());
        instrumento.setEstadoValidacion(
                datos.estadoValidacion() == null || datos.estadoValidacion().isBlank()
                        ? "BORRADOR" : datos.estadoValidacion());
    }

    // "Atención y energía" -> ATENCION_Y_ENERGIA (el motor usa este código en sus reglas)
    private String codigoSubescala(String texto) {
        if (texto == null || texto.isBlank()) {
            return "GENERAL";
        }
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String codigo = sinTildes.toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (codigo.isEmpty()) {
            return "GENERAL";
        }
        return codigo.substring(0, Math.min(30, codigo.length()));
    }

    private Regla buscarRegla(List<Regla> reglas, String codigo) {
        return reglas.stream().filter(r -> codigo.equals(r.getCodigo())).findFirst().orElse(null);
    }

    private Regla nuevaRegla(Instrumento instrumento, String codigo, String nivel, String recomendacion) {
        Regla regla = new Regla();
        regla.setInstrumento(instrumento);
        regla.setCodigo(codigo);
        regla.setDescripcion(codigo);
        regla.setNivel(nivel);
        regla.setRecomendacion(recomendacion);
        instrumento.getReglas().add(regla);
        return regla;
    }

    private void agregarCondicion(Regla regla, String operador, int valor) {
        CondicionRegla condicion = new CondicionRegla();
        condicion.setRegla(regla);
        condicion.setHecho("TOTAL.SINTOMAS");
        condicion.setOperador(operador);
        condicion.setValor(BigDecimal.valueOf(valor));
        regla.getCondiciones().add(condicion);
    }

    private Integer umbral(List<Regla> reglas, String codigo) {
        Regla regla = buscarRegla(reglas, codigo);
        if (regla == null) {
            return null;
        }
        return regla.getCondiciones().stream()
                .filter(c -> ">=".equals(c.getOperador()))
                .map(c -> c.getValor().intValue())
                .findFirst()
                .orElse(null);
    }

    private TestDetalle aDetalle(Instrumento i) {
        List<Pregunta> activas = activas(i);
        List<Regla> reglas = i.getReglas();
        boolean avanzadas = reglas.stream().anyMatch(r -> !r.getCodigo().startsWith("R_AUTO_"));
        Integer moderado = umbral(reglas, COD_MODERADO);
        Integer alto = umbral(reglas, COD_ALTO);

        String advertencia = null;
        if (!avanzadas && alto != null && alto > activas.size()) {
            advertencia = "El umbral alto (" + alto + ") es mayor que la cantidad de preguntas ("
                    + activas.size() + "). Ajusta los umbrales.";
        }

        return new TestDetalle(i.getId(), i.getCodigo(), i.getNombre(), i.getDescripcion(), i.getTrastorno(),
                i.getQuienResponde(), i.getEstadoValidacion(),
                i.getId() != null && evaluacionRepository.existePorInstrumento(i.getId()),
                !avanzadas, moderado, alto, advertencia,
                activas.stream()
                        .map(p -> new PreguntaTest(p.getId(), p.getOrden(), p.getTexto(), p.getSubescala()))
                        .toList(),
                reglas.stream().map(this::aRegla).toList());
    }

    private ReglaTest aRegla(Regla r) {
        return new ReglaTest(r.getCodigo(), r.getDescripcion(), r.getNivel(), r.getHechoDerivado(),
                r.getCondiciones().stream()
                        .map(c -> new CondicionTest(c.getHecho(), c.getOperador(), c.getValor().doubleValue()))
                        .toList());
    }
}