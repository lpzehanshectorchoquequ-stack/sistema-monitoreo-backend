package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.domain.HorarioAtencion;
import com.integrador.sistema_monitoreo_backend.domain.HorarioRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class HorarioRepositoryAdapter implements HorarioRepository {

    private final HorarioRepositoryJPA jpa;

    public HorarioRepositoryAdapter(HorarioRepositoryJPA jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<HorarioAtencion> listarPorPsicologo(Integer psicologoId) {
        return jpa.findByPsicologo_IdOrderByDiaSemanaAscHoraInicioAsc(psicologoId);
    }

    @Override
    public Optional<HorarioAtencion> buscarPorId(Integer id) {
        return jpa.findById(id);
    }

    @Override
    public HorarioAtencion guardar(HorarioAtencion horario) {
        return jpa.save(horario);
    }
}