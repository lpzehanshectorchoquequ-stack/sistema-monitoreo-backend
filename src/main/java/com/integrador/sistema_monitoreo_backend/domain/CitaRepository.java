package com.integrador.sistema_monitoreo_backend.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CitaRepository {
    Optional<Cita> buscarPorId(Integer id);
    Cita guardar(Cita cita);
    List<Cita> listarTodas();
    List<Cita> listarPorPsicologo(String correoPsicologo);
    List<Cita> listarPorPacientes(Collection<Integer> pacienteIds);
    List<Cita> listarEntre(Integer psicologoId, LocalDate desde, LocalDate hasta, Collection<String> estados);
}