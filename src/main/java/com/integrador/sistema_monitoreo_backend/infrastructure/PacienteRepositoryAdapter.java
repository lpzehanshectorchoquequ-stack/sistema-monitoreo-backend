package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.domain.Paciente;
import com.integrador.sistema_monitoreo_backend.domain.PacienteRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PacienteRepositoryAdapter implements PacienteRepository {

    private final PacienteRepositoryJPA jpa;

    public PacienteRepositoryAdapter(PacienteRepositoryJPA jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Paciente> listarTodos() {
        return jpa.findAll(Sort.by("nombre"));
    }

    @Override
    public List<Paciente> listarPorPsicologo(String correoPsicologo) {
        return jpa.findByPsicologo_CorreoOrderByNombreAsc(correoPsicologo);
    }

    @Override
    public List<Paciente> listarPorTutor(String correoTutor) {
        return jpa.buscarPorTutor(correoTutor);
    }

    @Override
    public Optional<Paciente> buscarPorId(Integer id) {
        return jpa.findById(id);
    }

    @Override
    public Paciente guardar(Paciente paciente) {
        return jpa.save(paciente);
    }
}