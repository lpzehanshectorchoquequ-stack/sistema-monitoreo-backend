package com.integrador.sistema_monitoreo_backend.application;

import com.integrador.sistema_monitoreo_backend.domain.CredencialesInvalidasException;
import com.integrador.sistema_monitoreo_backend.domain.Rol;
import com.integrador.sistema_monitoreo_backend.domain.TokenProvider;
import com.integrador.sistema_monitoreo_backend.domain.Usuario;
import com.integrador.sistema_monitoreo_backend.domain.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    private static final String CORREO = "psicologa@sistema.com";
    private static final String HASH = "$2a$hash-simulado";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenProvider tokenProvider;

    @InjectMocks
    private LoginUseCase loginUseCase;

    private Usuario crearUsuario(boolean activo, boolean contrasenaTemporal) {
        Rol rol = new Rol();
        rol.setNombreRol("Psicologo");
        Usuario usuario = new Usuario();
        usuario.setNombre("Psicologa Prueba");
        usuario.setCorreo(CORREO);
        usuario.setContrasena(HASH);
        usuario.setRol(rol);
        usuario.setActivo(activo);
        usuario.setDebeCambiarContrasena(contrasenaTemporal);
        return usuario;
    }

    @Test
    @DisplayName("Credenciales correctas: devuelve token y datos del usuario")
    void credencialesCorrectas_devuelveTokenYDatos() {
        // Arrange
        Usuario usuario = crearUsuario(true, false);
        when(usuarioRepository.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Clave12345", HASH)).thenReturn(true);
        when(tokenProvider.generarToken(usuario)).thenReturn("token-simulado");

        // Act
        LoginUseCase.Resultado resultado = loginUseCase.ejecutar(CORREO, "Clave12345");

        // Assert
        assertThat(resultado.token()).isEqualTo("token-simulado");
        assertThat(resultado.correo()).isEqualTo(CORREO);
        assertThat(resultado.rol()).isEqualTo("Psicologo");
        assertThat(resultado.debeCambiarContrasena()).isFalse();
    }

    @Test
    @DisplayName("Contrasena incorrecta: lanza excepcion y no genera token")
    void contrasenaIncorrecta_lanzaExcepcion() {
        // Arrange
        Usuario usuario = crearUsuario(true, false);
        when(usuarioRepository.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta", HASH)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> loginUseCase.ejecutar(CORREO, "incorrecta"))
                .isInstanceOf(CredencialesInvalidasException.class);
        verify(tokenProvider, never()).generarToken(any());
    }

    @Test
    @DisplayName("Usuario inexistente: lanza la misma excepcion que una contrasena mala")
    void usuarioInexistente_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.buscarPorCorreo("nadie@sistema.com")).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> loginUseCase.ejecutar("nadie@sistema.com", "Clave12345"))
                .isInstanceOf(CredencialesInvalidasException.class);
        verify(tokenProvider, never()).generarToken(any());
    }

    @Test
    @DisplayName("Cuenta desactivada: no entra aunque la contrasena sea correcta")
    void cuentaDesactivada_noPuedeEntrar() {
        // Arrange
        Usuario usuario = crearUsuario(false, false);
        when(usuarioRepository.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario));

        // Act + Assert
        assertThatThrownBy(() -> loginUseCase.ejecutar(CORREO, "Clave12345"))
                .isInstanceOf(CredencialesInvalidasException.class);
        verify(passwordEncoder, never()).matches(any(), any());
        verify(tokenProvider, never()).generarToken(any());
    }

    @Test
    @DisplayName("Cuenta con contrasena temporal: el resultado exige cambiarla")
    void contrasenaTemporal_indicaQueDebeCambiarla() {
        // Arrange
        Usuario usuario = crearUsuario(true, true);
        when(usuarioRepository.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Temporal2026", HASH)).thenReturn(true);
        when(tokenProvider.generarToken(usuario)).thenReturn("token-temporal");

        // Act
        LoginUseCase.Resultado resultado = loginUseCase.ejecutar(CORREO, "Temporal2026");

        // Assert
        assertThat(resultado.debeCambiarContrasena()).isTrue();
        assertThat(resultado.token()).isEqualTo("token-temporal");
    }
}