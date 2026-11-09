package com.integrador.sistema_monitoreo_backend.presentation;

import com.integrador.sistema_monitoreo_backend.application.GestionarUsuariosUseCase;
import com.integrador.sistema_monitoreo_backend.application.GestionarUsuariosUseCase.UsuarioCreado;
import com.integrador.sistema_monitoreo_backend.application.GestionarUsuariosUseCase.UsuarioVista;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    public record CrearUsuarioRequest(
            @NotBlank(message = "Ingresa el nombre")
            @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
            @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MSG_NOMBRE) String nombre,

            @NotBlank(message = "Ingresa el correo")
            @Size(max = 100, message = "El correo admite hasta 100 caracteres")
            @Pattern(regexp = Validaciones.CORREO, message = Validaciones.MSG_CORREO) String correo,

            @NotBlank(message = "Selecciona un rol")
            @Pattern(regexp = "^(Admin|Psicologo|Tutor)$", message = "Rol no válido") String rol
    ) {}

    public record EstadoRequest(@NotNull Boolean activo) {}

    private final GestionarUsuariosUseCase useCase;

    public UsuarioController(GestionarUsuariosUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public List<UsuarioVista> listar(Authentication auth) {
        return useCase.listar(auth.getName(), rol(auth));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioCreado crear(@Valid @RequestBody CrearUsuarioRequest r, Authentication auth) {
        return useCase.crear(r.nombre(), r.correo(), r.rol(), auth.getName(), rol(auth));
    }

    @PostMapping("/{id}/restablecer-contrasena")
    public UsuarioCreado restablecer(@PathVariable Integer id, Authentication auth) {
        return useCase.restablecerContrasena(id, auth.getName(), rol(auth));
    }

    @PutMapping("/{id}/estado")
    public UsuarioVista cambiarEstado(@PathVariable Integer id,
                                      @Valid @RequestBody EstadoRequest r,
                                      Authentication auth) {
        return useCase.cambiarEstado(id, r.activo(), auth.getName(), rol(auth));
    }

    private String rol(Authentication auth) {
        return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }
}