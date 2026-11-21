package com.integrador.sistema_monitoreo_backend.domain;

import java.util.List;

public interface EvaluacionRepository {
    Evaluacion guardar(Evaluacion evaluacion);
    List<Evaluacion> listarPorPaciente(Integer pacienteId);
    List<Evaluacion> listarTodas();
    boolean existePorInstrumento(Integer instrumentoId);
}