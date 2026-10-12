package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.domain.Rol;
import com.integrador.sistema_monitoreo_backend.domain.RolRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RolRepositoryAdapter implements RolRepository {

    private final RolRepositoryJPA rolRepositoryJPA;

    public RolRepositoryAdapter(RolRepositoryJPA rolRepositoryJPA) {
        this.rolRepositoryJPA = rolRepositoryJPA;
    }

    @Override
    public List<Rol> obtenerTodos() {
        return rolRepositoryJPA.findAll();
    }

    @Override
    public Optional<Rol> buscarPorNombre(String nombreRol) {
        return rolRepositoryJPA.findByNombreRol(nombreRol);
    }
}