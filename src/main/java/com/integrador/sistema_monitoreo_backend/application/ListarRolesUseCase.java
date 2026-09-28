package com.integrador.sistema_monitoreo_backend.application;

import com.integrador.sistema_monitoreo_backend.domain.Rol;
import com.integrador.sistema_monitoreo_backend.domain.RolRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ListarRolesUseCase {

    private final RolRepository rolRepository;

    public ListarRolesUseCase(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    public List<Rol> ejecutar() {
        return rolRepository.obtenerTodos();
    }
}