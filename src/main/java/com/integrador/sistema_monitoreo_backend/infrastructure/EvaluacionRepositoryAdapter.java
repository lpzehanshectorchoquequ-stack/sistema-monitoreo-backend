package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.domain.Evaluacion;
import com.integrador.sistema_monitoreo_backend.domain.EvaluacionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EvaluacionRepositoryAdapter implements EvaluacionRepository {

    private final EvaluacionRepositoryJPA jpa;

    public EvaluacionRepositoryAdapter(EvaluacionRepositoryJPA jpa) {
        this.jpa = jpa;
    }

    @Override
    public Evaluacion guardar(Evaluacion evaluacion) {
        return jpa.save(evaluacion);
    }

    @Override
    public Optional<Evaluacion> buscarPorId(Integer id) {
        return jpa.findById(id);
    }

    @Override
    public List<Evaluacion> listarPorPaciente(Integer pacienteId) {
        return jpa.findByPaciente_IdOrderByFechaEvaluacionDescIdDesc(pacienteId);
    }

    @Override
    public List<Evaluacion> listarTodas() {
        return jpa.findAll();
    }

    @Override
    public boolean existePorInstrumento(Integer instrumentoId) {
        return jpa.existsByInstrumento_Id(instrumentoId);
    }
}