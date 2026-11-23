package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.domain.Cita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface CitaRepositoryJPA extends JpaRepository<Cita, Integer> {
    List<Cita> findByPsicologo_Correo(String correo);

    List<Cita> findByPaciente_IdIn(Collection<Integer> ids);

    List<Cita> findByPsicologo_IdAndFechaBetweenAndEstadoIn(
            Integer psicologoId, LocalDate desde, LocalDate hasta, Collection<String> estados);
}