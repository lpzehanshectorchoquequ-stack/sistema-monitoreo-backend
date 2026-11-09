package com.integrador.sistema_monitoreo_backend.application;

import com.integrador.sistema_monitoreo_backend.domain.CredencialesInvalidasException;
import com.integrador.sistema_monitoreo_backend.domain.ReglaNegocioException;
import com.integrador.sistema_monitoreo_backend.domain.TokenProvider;
import com.integrador.sistema_monitoreo_backend.domain.Usuario;
import com.integrador.sistema_monitoreo_backend.domain.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CambiarContrasenaUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public CambiarContrasenaUseCase(UsuarioRepository usuarioRepository,
                                    PasswordEncoder passwordEncoder,
                                    TokenProvider tokenProvider) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public LoginUseCase.Resultado ejecutar(String correo, String contrasenaActual, String contrasenaNueva) {
        Usuario usuario = usuarioRepository.buscarPorCorreo(correo)
                .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordEncoder.matches(contrasenaActual, usuario.getContrasena())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta");
        }
        if (contrasenaNueva.length() < 8) {
            throw new ReglaNegocioException("La nueva contraseña debe tener al menos 8 caracteres");
        }
        if (contrasenaNueva.equals(contrasenaActual)) {
            throw new ReglaNegocioException("La nueva contraseña debe ser diferente a la actual");
        }

        usuario.setContrasena(passwordEncoder.encode(contrasenaNueva));
        usuario.setDebeCambiarContrasena(false);
        Usuario guardado = usuarioRepository.guardar(usuario);

        // Token nuevo, ya sin la marca de contraseña temporal
        return new LoginUseCase.Resultado(tokenProvider.generarToken(guardado), guardado.getNombre(),
                guardado.getCorreo(), guardado.getRol().getNombreRol(), false);
    }
}