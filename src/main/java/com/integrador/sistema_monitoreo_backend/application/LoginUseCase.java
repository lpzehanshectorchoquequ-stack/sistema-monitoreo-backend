package com.integrador.sistema_monitoreo_backend.application;

import com.integrador.sistema_monitoreo_backend.domain.CredencialesInvalidasException;
import com.integrador.sistema_monitoreo_backend.domain.TokenProvider;
import com.integrador.sistema_monitoreo_backend.domain.Usuario;
import com.integrador.sistema_monitoreo_backend.domain.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginUseCase {

    public record Resultado(String token, String nombre, String correo, String rol) {}

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public LoginUseCase(UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder,
                        TokenProvider tokenProvider) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public Resultado ejecutar(String correo, String contrasena) {
        Usuario usuario = usuarioRepository.buscarPorCorreo(correo)
                .orElseThrow(CredencialesInvalidasException::new);

        if (!usuario.getActivo() || !passwordEncoder.matches(contrasena, usuario.getContrasena())) {
            throw new CredencialesInvalidasException();
        }

        String token = tokenProvider.generarToken(usuario);
        return new Resultado(token, usuario.getNombre(), usuario.getCorreo(), usuario.getRol().getNombreRol());
    }
}