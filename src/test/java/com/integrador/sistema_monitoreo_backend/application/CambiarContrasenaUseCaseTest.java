package com.integrador.sistema_monitoreo_backend.application;

import com.integrador.sistema_monitoreo_backend.domain.CredencialesInvalidasException;
import com.integrador.sistema_monitoreo_backend.domain.ReglaNegocioException;
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
class CambiarContrasenaUseCaseTest {

    private static final String CORREO = "familia@sistema.com";
    private static final String HASH_TEMPORAL = "hash-temporal";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenProvider tokenProvider;

    @InjectMocks
    private CambiarContrasenaUseCase useCase;

    private Usuario usuarioConContrasenaTemporal() {
        Rol rol = new Rol();
        rol.setNombreRol("Tutor");
        Usuario usuario = new Usuario();
        usuario.setNombre("Familia Prueba");
        usuario.setCorreo(CORREO);
        usuario.setContrasena(HASH_TEMPORAL);
        usuario.setRol(rol);
        usuario.setDebeCambiarContrasena(true);
        return usuario;
    }

    @Test
    @DisplayName("Cambio correcto: guarda la clave cifrada, quita la marca temporal y da token nuevo")
    void cambioCorrecto_guardaClaveCifradaYEntregaTokenNuevo() {
        // Arrange
        Usuario usuario = usuarioConContrasenaTemporal();
        when(usuarioRepository.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Temporal2026", HASH_TEMPORAL)).thenReturn(true);
        when(passwordEncoder.encode("MiClaveNueva2026")).thenReturn("hash-nuevo");
        when(usuarioRepository.guardar(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(tokenProvider.generarToken(any(Usuario.class))).thenReturn("token-nuevo");

        // Act
        LoginUseCase.Resultado resultado = useCase.ejecutar(CORREO, "Temporal2026", "MiClaveNueva2026");

        // Assert
        assertThat(resultado.token()).isEqualTo("token-nuevo");
        assertThat(resultado.debeCambiarContrasena()).isFalse();
        assertThat(usuario.getContrasena()).isEqualTo("hash-nuevo");
        assertThat(usuario.getDebeCambiarContrasena()).isFalse();
    }

    @Test
    @DisplayName("Contrasena actual incorrecta: se rechaza y no se guarda")
    void contrasenaActualIncorrecta_seRechaza() {
        // Arrange
        Usuario usuario = usuarioConContrasenaTemporal();
        when(usuarioRepository.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("equivocada", HASH_TEMPORAL)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> useCase.ejecutar(CORREO, "equivocada", "MiClaveNueva2026"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no es correcta");
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Contrasena nueva demasiado corta: se rechaza")
    void contrasenaNuevaMuyCorta_seRechaza() {
        // Arrange
        Usuario usuario = usuarioConContrasenaTemporal();
        when(usuarioRepository.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Temporal2026", HASH_TEMPORAL)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> useCase.ejecutar(CORREO, "Temporal2026", "abc12"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("al menos 8");
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Contrasena nueva igual a la actual: se rechaza")
    void contrasenaNuevaIgualALaActual_seRechaza() {
        // Arrange
        Usuario usuario = usuarioConContrasenaTemporal();
        when(usuarioRepository.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("MismaClave123", HASH_TEMPORAL)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> useCase.ejecutar(CORREO, "MismaClave123", "MismaClave123"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("diferente");
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Usuario inexistente: lanza excepcion de credenciales")
    void usuarioInexistente_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.buscarPorCorreo("nadie@sistema.com")).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> useCase.ejecutar("nadie@sistema.com", "Temporal2026", "MiClaveNueva2026"))
                .isInstanceOf(CredencialesInvalidasException.class);
        verify(tokenProvider, never()).generarToken(any());
    }
}