package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.domain.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PacienteRepositoryJPA extends JpaRepository<Paciente, Integer> {

    List<Paciente> findByPsicologo_CorreoOrderByNombreAsc(String correo);

    @Query("""
            select distinct p from Paciente p
            left join p.tutor t
            left join p.vinculos v
            left join v.tutor vt
            where t.correo = :correo
               or (vt.correo = :correo and v.eliminado = false)
            order by p.nombre asc
            """)
    List<Paciente> buscarPorTutor(@Param("correo") String correo);
}