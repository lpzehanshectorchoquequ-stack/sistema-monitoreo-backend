package com.integrador.sistema_monitoreo_backend.domain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MotorInferencia {

    // Una respuesta de 2 o 3 cuenta como "síntoma presente"
    public static final int UMBRAL_SINTOMA = 2;

    private static final String RECOMENDACION_SIN_INDICADORES =
            "No se observan indicadores significativos con este instrumento. "
            + "Se sugiere continuar con la observación habitual.";

    private static final Map<String, Integer> ORDEN_NIVEL = Map.of("BAJO", 1, "MODERADO", 2, "ALTO", 3);

    public record Resultado(String trastorno, String nivel, String recomendacion, List<String> explicacion) {}

    public Resultado evaluar(Instrumento instrumento, Map<Integer, Integer> respuestas) {
        Map<String, Double> hechos = calcularHechos(instrumento, respuestas);
        List<Regla> activadas = encadenarHaciaAdelante(instrumento.getReglas(), hechos);
        return concluir(instrumento, hechos, activadas);
    }

    // 1) Base de hechos: convierte las respuestas en datos que las reglas pueden evaluar
    private Map<String, Double> calcularHechos(Instrumento instrumento, Map<Integer, Integer> respuestas) {
        Map<String, Double> hechos = new LinkedHashMap<>();
        for (Pregunta pregunta : instrumento.getPreguntas()) {
            int valor = respuestas.get(pregunta.getId());
            double sintoma = valor >= UMBRAL_SINTOMA ? 1.0 : 0.0;
            hechos.merge(pregunta.getSubescala() + ".SINTOMAS", sintoma, Double::sum);
            hechos.merge(pregunta.getSubescala() + ".PUNTAJE", (double) valor, Double::sum);
            hechos.merge("TOTAL.SINTOMAS", sintoma, Double::sum);
            hechos.merge("TOTAL.PUNTAJE", (double) valor, Double::sum);
        }
        return hechos;
    }

    // 2) Encadenamiento hacia adelante: repite rondas hasta que ninguna regla nueva se active.
    //    Una regla puede producir un hecho nuevo que permite activar otras en la ronda siguiente.
    private List<Regla> encadenarHaciaAdelante(List<Regla> reglas, Map<String, Double> hechos) {
        List<Regla> pendientes = new ArrayList<>(reglas);
        List<Regla> activadas = new ArrayList<>();
        boolean huboCambios;
        do {
            huboCambios = false;
            Iterator<Regla> iterador = pendientes.iterator();
            while (iterador.hasNext()) {
                Regla regla = iterador.next();
                if (cumple(regla, hechos)) {
                    activadas.add(regla);
                    if (regla.getHechoDerivado() != null) {
                        hechos.put(regla.getHechoDerivado(), 1.0);
                    }
                    iterador.remove();
                    huboCambios = true;
                }
            }
        } while (huboCambios);
        return activadas;
    }

    private boolean cumple(Regla regla, Map<String, Double> hechos) {
        if (regla.getCondiciones().isEmpty()) {
            return false;
        }
        for (CondicionRegla condicion : regla.getCondiciones()) {
            double actual = hechos.getOrDefault(condicion.getHecho(), 0.0);
            double esperado = condicion.getValor().doubleValue();
            boolean ok = switch (condicion.getOperador()) {
                case ">=" -> actual >= esperado;
                case ">" -> actual > esperado;
                case "<=" -> actual <= esperado;
                case "<" -> actual < esperado;
                case "==" -> actual == esperado;
                default -> false;
            };
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    // 3) Resolución de conflictos y explicación: gana el nivel más alto
    private Resultado concluir(Instrumento instrumento, Map<String, Double> hechos, List<Regla> activadas) {
        int maximo = 0;
        for (Regla regla : activadas) {
            if (regla.getNivel() != null) {
                maximo = Math.max(maximo, ORDEN_NIVEL.getOrDefault(regla.getNivel(), 0));
            }
        }

        String nivel = "SIN_INDICADORES";
        List<Regla> determinantes = new ArrayList<>();
        Set<String> recomendaciones = new LinkedHashSet<>();
        if (maximo > 0) {
            for (Regla regla : activadas) {
                if (regla.getNivel() != null && ORDEN_NIVEL.getOrDefault(regla.getNivel(), 0) == maximo) {
                    nivel = regla.getNivel();
                    determinantes.add(regla);
                    if (regla.getRecomendacion() != null) {
                        recomendaciones.add(regla.getRecomendacion());
                    }
                }
            }
        }
        String recomendacion = maximo == 0 ? RECOMENDACION_SIN_INDICADORES : String.join(" ", recomendaciones);

        List<String> explicacion = new ArrayList<>();
        explicacion.add("Hechos calculados: " + formatearHechos(hechos));
        for (Regla regla : activadas) {
            if (regla.getHechoDerivado() != null) {
                explicacion.add("Hecho inferido " + regla.getHechoDerivado() + " por la regla "
                        + regla.getCodigo() + ": " + regla.getDescripcion() + " (" + detalle(regla, hechos) + ")");
            }
        }
        for (Regla regla : determinantes) {
            explicacion.add("Conclusión " + regla.getNivel() + " por la regla " + regla.getCodigo() + ": "
                    + regla.getDescripcion() + " (" + detalle(regla, hechos) + ")");
        }
        if (maximo == 0) {
            explicacion.add("Ninguna regla de conclusión se cumplió con las respuestas dadas.");
        }
        return new Resultado(instrumento.getTrastorno(), nivel, recomendacion, explicacion);
    }

    private String detalle(Regla regla, Map<String, Double> hechos) {
        List<String> partes = new ArrayList<>();
        for (CondicionRegla c : regla.getCondiciones()) {
            partes.add(c.getHecho() + " (" + numero(hechos.getOrDefault(c.getHecho(), 0.0)) + ") "
                    + c.getOperador() + " " + numero(c.getValor().doubleValue()));
        }
        return "se cumplió: " + String.join(" y ", partes);
    }

    private String formatearHechos(Map<String, Double> hechos) {
        List<String> partes = new ArrayList<>();
        hechos.forEach((nombre, valor) -> partes.add(nombre + " = " + numero(valor)));
        return String.join(", ", partes);
    }

    private String numero(double valor) {
        return valor == Math.rint(valor) ? String.valueOf((long) valor) : String.valueOf(valor);
    }
}