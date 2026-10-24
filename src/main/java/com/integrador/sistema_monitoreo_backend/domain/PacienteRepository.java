package com.integrador.sistema_monitoreo_backend.domain;

import java.util.List;
import java.util.Optional;

public interface PacienteRepository {
    List<Paciente> listarTodos();
    List<Paciente> listarPorPsicologo(String correoPsicologo);
    List<Paciente> listarPorTutor(String correoTutor);
    Optional<Paciente> buscarPorId(Integer id);
    Paciente guardar(Paciente paciente);
}