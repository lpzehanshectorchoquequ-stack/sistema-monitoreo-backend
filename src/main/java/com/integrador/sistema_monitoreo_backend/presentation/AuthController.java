package com.integrador.sistema_monitoreo_backend.presentation;

import com.integrador.sistema_monitoreo_backend.application.CambiarContrasenaUseCase;
import com.integrador.sistema_monitoreo_backend.application.LoginUseCase;
import com.integrador.sistema_monitoreo_backend.application.RegistrarBitacoraUseCase;
import com.integrador.sistema_monitoreo_backend.domain.CredencialesInvalidasException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(
            @NotBlank(message = "Ingresa tu correo")
            @Size(max = 100, message = "El correo admite hasta 100 caracteres")
            @Pattern(regexp = Validaciones.CORREO, message = Validaciones.MSG_CORREO) String correo,
            @NotBlank(message = "Ingresa tu contraseña")
            @Size(max = 100, message = "La contraseña admite hasta 100 caracteres") String contrasena) {}

    public record CambioContrasenaRequest(
            @NotBlank(message = "Ingresa tu contraseña actual")
            @Size(max = 100, message = "La contraseña admite hasta 100 caracteres") String contrasenaActual,
            @NotBlank(message = "Ingresa la nueva contraseña")
            @Pattern(regexp = Validaciones.CONTRASENA, message = Validaciones.MSG_CONTRASENA) String contrasenaNueva) {}

    public record LoginResponse(String token, String nombre, String correo, String rol,
                                boolean debeCambiarContrasena) {}

    private final LoginUseCase loginUseCase;
    private final CambiarContrasenaUseCase cambiarContrasenaUseCase;
    private final RegistrarBitacoraUseCase bitacora;

    public AuthController(LoginUseCase loginUseCase,
                          CambiarContrasenaUseCase cambiarContrasenaUseCase,
                          RegistrarBitacoraUseCase bitacora) {
        this.loginUseCase = loginUseCase;
        this.cambiarContrasenaUseCase = cambiarContrasenaUseCase;
        this.bitacora = bitacora;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        try {
            LoginUseCase.Resultado resultado = loginUseCase.ejecutar(request.correo(), request.contrasena());
            bitacora.registrar(request.correo(), resultado.rol(), "Sesión", "INICIO_SESION",
                    "Inició sesión", "POST", "/api/auth/login", 200, http.getRemoteAddr());
            return aRespuesta(resultado);
        } catch (CredencialesInvalidasException e) {
            bitacora.registrar(request.correo(), null, "Sesión", "SESION_FALLIDA",
                    "Intento fallido de inicio de sesión", "POST", "/api/auth/login", 401, http.getRemoteAddr());
            throw e;
        }
    }

    @PostMapping("/cambiar-contrasena")
    public LoginResponse cambiarContrasena(@Valid @RequestBody CambioContrasenaRequest request,
                                           Authentication auth) {
        return aRespuesta(cambiarContrasenaUseCase.ejecutar(
                auth.getName(), request.contrasenaActual(), request.contrasenaNueva()));
    }

    private LoginResponse aRespuesta(LoginUseCase.Resultado r) {
        return new LoginResponse(r.token(), r.nombre(), r.correo(), r.rol(), r.debeCambiarContrasena());
    }
}