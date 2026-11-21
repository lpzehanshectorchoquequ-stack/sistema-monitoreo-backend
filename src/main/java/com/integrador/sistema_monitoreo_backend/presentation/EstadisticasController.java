package com.integrador.sistema_monitoreo_backend.presentation;

import com.integrador.sistema_monitoreo_backend.application.ConsultarEstadisticasUseCase;
import com.integrador.sistema_monitoreo_backend.application.ConsultarEstadisticasUseCase.EvolucionVista;
import com.integrador.sistema_monitoreo_backend.application.ConsultarEstadisticasUseCase.ResumenVista;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    private final ConsultarEstadisticasUseCase useCase;

    public EstadisticasController(ConsultarEstadisticasUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/resumen")
    public ResumenVista resumen(Authentication auth) {
        return useCase.resumen(auth.getName(), rol(auth));
    }

    @GetMapping("/pacientes/{id}/evolucion")
    public EvolucionVista evolucion(@PathVariable Integer id, Authentication auth) {
        return useCase.evolucion(id, auth.getName(), rol(auth));
    }

    private String rol(Authentication auth) {
        return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }
}