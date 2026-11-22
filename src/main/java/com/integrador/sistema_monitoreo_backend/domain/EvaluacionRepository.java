package com.integrador.sistema_monitoreo_backend.domain;

import java.util.List;
import java.util.Optional;

public interface EvaluacionRepository {
    Evaluacion guardar(Evaluacion evaluacion);
    Optional<Evaluacion> buscarPorId(Integer id);
    List<Evaluacion> listarPorPaciente(Integer pacienteId);
    List<Evaluacion> listarTodas();
    boolean existePorInstrumento(Integer instrumentoId);
}