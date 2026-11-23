package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.domain.HorarioAtencion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioRepositoryJPA extends JpaRepository<HorarioAtencion, Integer> {
    List<HorarioAtencion> findByPsicologo_IdOrderByDiaSemanaAscHoraInicioAsc(Integer psicologoId);
}