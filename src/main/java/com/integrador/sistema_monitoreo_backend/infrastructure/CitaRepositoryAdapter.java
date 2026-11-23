package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.domain.Cita;
import com.integrador.sistema_monitoreo_backend.domain.CitaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class CitaRepositoryAdapter implements CitaRepository {

    private final CitaRepositoryJPA jpa;

    public CitaRepositoryAdapter(CitaRepositoryJPA jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Cita> buscarPorId(Integer id) {
        return jpa.findById(id);
    }

    @Override
    public Cita guardar(Cita cita) {
        return jpa.save(cita);
    }

    @Override
    public List<Cita> listarTodas() {
        return jpa.findAll();
    }

    @Override
    public List<Cita> listarPorPsicologo(String correoPsicologo) {
        return jpa.findByPsicologo_Correo(correoPsicologo);
    }

    @Override
    public List<Cita> listarPorPacientes(Collection<Integer> pacienteIds) {
        return jpa.findByPaciente_IdIn(pacienteIds);
    }

    @Override
    public List<Cita> listarEntre(Integer psicologoId, LocalDate desde, LocalDate hasta, Collection<String> estados) {
        return jpa.findByPsicologo_IdAndFechaBetweenAndEstadoIn(psicologoId, desde, hasta, estados);
    }
}