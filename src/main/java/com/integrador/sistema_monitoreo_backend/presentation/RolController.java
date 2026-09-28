package com.integrador.sistema_monitoreo_backend.presentation;

import com.integrador.sistema_monitoreo_backend.application.ListarRolesUseCase;
import com.integrador.sistema_monitoreo_backend.domain.Rol;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RolController {

    private final ListarRolesUseCase listarRolesUseCase;

    public RolController(ListarRolesUseCase listarRolesUseCase) {
        this.listarRolesUseCase = listarRolesUseCase;
    }

    @GetMapping
    public List<Rol> listar() {
        return listarRolesUseCase.ejecutar();
    }
}