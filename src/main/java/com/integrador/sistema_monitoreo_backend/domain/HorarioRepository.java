package com.integrador.sistema_monitoreo_backend.domain;

import java.util.List;
import java.util.Optional;

public interface HorarioRepository {
    List<HorarioAtencion> listarPorPsicologo(Integer psicologoId);
    Optional<HorarioAtencion> buscarPorId(Integer id);
    HorarioAtencion guardar(HorarioAtencion horario);
}