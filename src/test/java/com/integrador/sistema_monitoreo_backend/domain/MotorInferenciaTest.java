package com.integrador.sistema_monitoreo_backend.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MotorInferenciaTest {

    private static final String REC_ALTO = "Derivar a evaluacion clinica completa";
    private static final String REC_MODERADO = "Dar seguimiento y repetir la evaluacion";

    private final MotorInferencia motor = new MotorInferencia();

    // ---------- Datos de prueba ----------

    private Pregunta pregunta(int id, String subescala) {
        Pregunta p = new Pregunta();
        ReflectionTestUtils.setField(p, "id", id);
        ReflectionTestUtils.setField(p, "orden", id);
        ReflectionTestUtils.setField(p, "texto", "Pregunta " + id);
        ReflectionTestUtils.setField(p, "subescala", subescala);
        return p;
    }

    private CondicionRegla condicion(String hecho, String operador, int valor) {
        CondicionRegla c = new CondicionRegla();
        ReflectionTestUtils.setField(c, "hecho", hecho);
        ReflectionTestUtils.setField(c, "operador", operador);
        ReflectionTestUtils.setField(c, "valor", BigDecimal.valueOf(valor));
        return c;
    }

    private Regla regla(String codigo, String nivel, String hechoDerivado, String recomendacion,
                        CondicionRegla... condiciones) {
        Regla r = new Regla();
        ReflectionTestUtils.setField(r, "codigo", codigo);
        ReflectionTestUtils.setField(r, "descripcion", "Descripcion de " + codigo);
        ReflectionTestUtils.setField(r, "nivel", nivel);
        ReflectionTestUtils.setField(r, "hechoDerivado", hechoDerivado);
        ReflectionTestUtils.setField(r, "recomendacion", recomendacion);
        r.getCondiciones().addAll(List.of(condiciones));
        return r;
    }

    // 3 preguntas de INATENCION (ids 1-3) y 3 de HIPERACTIVIDAD (ids 4-6)
    private Instrumento instrumentoTdah() {
        Instrumento instrumento = new Instrumento();
        ReflectionTestUtils.setField(instrumento, "codigo", "TDAH_PRUEBA");
        ReflectionTestUtils.setField(instrumento, "trastorno", "TDAH");
        for (int id = 1; id <= 3; id++) {
            instrumento.getPreguntas().add(pregunta(id, "INATENCION"));
        }
        for (int id = 4; id <= 6; id++) {
            instrumento.getPreguntas().add(pregunta(id, "HIPERACTIVIDAD"));
        }

        // La regla de conclusion va ANTES de la que produce el hecho que necesita
        instrumento.getReglas().add(regla("R_INATENTO", "ALTO", null, REC_ALTO,
                condicion("IND_INATENCION", ">=", 1),
                condicion("HIPERACTIVIDAD.SINTOMAS", "<", 3)));
        instrumento.getReglas().add(regla("R_INAT", null, "IND_INATENCION", null,
                condicion("INATENCION.SINTOMAS", ">=", 3)));
        instrumento.getReglas().add(regla("R_MOD_INAT", "MODERADO", null, REC_MODERADO,
                condicion("INATENCION.SINTOMAS", ">=", 2),
                condicion("INATENCION.SINTOMAS", "<", 3)));
        return instrumento;
    }

    private Map<Integer, Integer> respuestasDe(int... valores) {
        Map<Integer, Integer> mapa = new HashMap<>();
        for (int i = 0; i < valores.length; i++) {
            mapa.put(i + 1, valores[i]);
        }
        return mapa;
    }

    // ---------- Pruebas ----------

    @Test
    @DisplayName("Inatencion alta con poca hiperactividad: encadena reglas y concluye ALTO")
    void inatencionAlta_encadenaReglasYConcluyeAlto() {
        // Arrange
        Instrumento instrumento = instrumentoTdah();
        Map<Integer, Integer> respuestas = respuestasDe(3, 3, 3, 0, 0, 0);

        // Act
        MotorInferencia.Resultado resultado = motor.evaluar(instrumento, respuestas);

        // Assert
        assertThat(resultado.trastorno()).isEqualTo("TDAH");
        assertThat(resultado.nivel()).isEqualTo("ALTO");
        assertThat(resultado.recomendacion()).isEqualTo(REC_ALTO);
        assertThat(resultado.explicacion()).hasSize(3);
        assertThat(resultado.explicacion().get(1)).contains("IND_INATENCION").contains("R_INAT");
        assertThat(resultado.explicacion().get(2)).contains("R_INATENTO");
    }

    @Test
    @DisplayName("Dos sintomas de inatencion: concluye MODERADO")
    void dosSintomasDeInatencion_concluyeModerado() {
        // Arrange
        Instrumento instrumento = instrumentoTdah();
        Map<Integer, Integer> respuestas = respuestasDe(3, 3, 0, 0, 0, 0);

        // Act
        MotorInferencia.Resultado resultado = motor.evaluar(instrumento, respuestas);

        // Assert
        assertThat(resultado.nivel()).isEqualTo("MODERADO");
        assertThat(resultado.recomendacion()).isEqualTo(REC_MODERADO);
        assertThat(resultado.explicacion()).hasSize(2);
    }

    @Test
    @DisplayName("Todas las respuestas en cero: sin indicadores significativos")
    void todasEnCero_sinIndicadores() {
        // Arrange
        Instrumento instrumento = instrumentoTdah();
        Map<Integer, Integer> respuestas = respuestasDe(0, 0, 0, 0, 0, 0);

        // Act
        MotorInferencia.Resultado resultado = motor.evaluar(instrumento, respuestas);

        // Assert
        assertThat(resultado.nivel()).isEqualTo("SIN_INDICADORES");
        assertThat(resultado.recomendacion()).contains("No se observan indicadores significativos");
        assertThat(resultado.explicacion().get(1)).contains("Ninguna regla");
    }

    @Test
    @DisplayName("Una respuesta de valor 1 NO cuenta como sintoma (pero si suma puntaje)")
    void respuestaUno_noCuentaComoSintoma() {
        // Arrange
        Instrumento instrumento = instrumentoTdah();
        Map<Integer, Integer> respuestas = respuestasDe(1, 1, 1, 1, 1, 1);

        // Act
        MotorInferencia.Resultado resultado = motor.evaluar(instrumento, respuestas);

        // Assert
        assertThat(resultado.nivel()).isEqualTo("SIN_INDICADORES");
        assertThat(resultado.explicacion().get(0))
                .contains("INATENCION.SINTOMAS = 0")
                .contains("INATENCION.PUNTAJE = 3");
    }

    @Test
    @DisplayName("Valor limite: una respuesta de valor 2 SI cuenta como sintoma")
    void respuestaDos_siCuentaComoSintoma() {
        // Arrange
        Instrumento instrumento = instrumentoTdah();
        Map<Integer, Integer> respuestas = respuestasDe(2, 2, 2, 0, 0, 0);

        // Act
        MotorInferencia.Resultado resultado = motor.evaluar(instrumento, respuestas);

        // Assert
        assertThat(resultado.nivel()).isEqualTo("ALTO");
        assertThat(resultado.explicacion().get(0)).contains("INATENCION.SINTOMAS = 3");
    }
}