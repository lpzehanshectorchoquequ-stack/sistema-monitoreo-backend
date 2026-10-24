package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.domain.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PacienteRepositoryJPA extends JpaRepository<Paciente, Integer> {
    List<Paciente> findByPsicologo_CorreoOrderByNombreAsc(String correo);
    List<Paciente> findByTutor_CorreoOrderByNombreAsc(String correo);
}