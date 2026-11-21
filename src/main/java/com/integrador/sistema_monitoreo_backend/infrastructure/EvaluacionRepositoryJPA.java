package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.domain.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluacionRepositoryJPA extends JpaRepository<Evaluacion, Integer> {
    List<Evaluacion> findByPaciente_IdOrderByFechaEvaluacionDescIdDesc(Integer pacienteId);
    boolean existsByInstrumento_Id(Integer instrumentoId);
}