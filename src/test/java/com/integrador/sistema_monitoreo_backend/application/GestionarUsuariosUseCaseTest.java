package com.integrador.sistema_monitoreo_backend.application;

import com.integrador.sistema_monitoreo_backend.domain.AccesoDenegadoException;
import com.integrador.sistema_monitoreo_backend.domain.ReglaNegocioException;
import com.integrador.sistema_monitoreo_backend.domain.Rol;
import com.integrador.sistema_monitoreo_backend.domain.RolRepository;
import com.integrador.sistema_monitoreo_backend.domain.Usuario;
import com.integrador.sistema_monitoreo_backend.domain.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestionarUsuariosUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private GestionarUsuariosUseCase useCase;

    private Rol rol(String nombre) {
        Rol rol = new Rol();
        rol.setNombreRol(nombre);
        return rol;
    }

    private Usuario usuario(String correo, String nombreRol) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Nombre " + nombreRol);
        usuario.setCorreo(correo);
        usuario.setRol(rol(nombreRol));
        return usuario;
    }

    @Test
    @DisplayName("Psicologa crea un tutor: contrasena temporal valida, cifrada y ligada a ella")
    void psicologaCreaTutor_generaContrasenaTemporalCifrada() {
        // Arrange
        Usuario psicologa = usuario("psicologa@sistema.com", "Psicologo");
        when(usuarioRepository.buscarPorCorreo("psicologa@sistema.com")).thenReturn(Optional.of(psicologa));
        when(rolRepository.buscarPorNombre("Tutor")).thenReturn(Optional.of(rol("Tutor")));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-simulado");
        when(usuarioRepository.guardar(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act
        GestionarUsuariosUseCase.UsuarioCreado creado = useCase.crear(
                "Familia Rojas", "familia.rojas@sistema.com", "Tutor", "psicologa@sistema.com", "PSICOLOGO");

        // Assert
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).guardar(guardado.capture());
        ArgumentCaptor<String> cifrada = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(cifrada.capture());

        assertThat(creado.contrasenaTemporal()).matches("^[A-HJ-NP-Za-km-np-z2-9]{10}$");
        assertThat(cifrada.getValue()).isEqualTo(creado.contrasenaTemporal());
        assertThat(guardado.getValue().getContrasena())
                .isEqualTo("hash-simulado")
                .isNotEqualTo(creado.contrasenaTemporal());
        assertThat(guardado.getValue().getDebeCambiarContrasena()).isTrue();
        assertThat(guardado.getValue().getPsicologo()).isSameAs(psicologa);
        assertThat(creado.usuario().rol()).isEqualTo("Tutor");
    }

    @Test
    @DisplayName("Una psicologa NO puede crear otra psicologa")
    void psicologaNoPuedeCrearPsicologa() {
        // Arrange
        Usuario psicologa = usuario("psicologa@sistema.com", "Psicologo");
        when(usuarioRepository.buscarPorCorreo("psicologa@sistema.com")).thenReturn(Optional.of(psicologa));

        // Act + Assert
        assertThatThrownBy(() -> useCase.crear(
                "Otra Psicologa", "otra@sistema.com", "Psicologo", "psicologa@sistema.com", "PSICOLOGO"))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessageContaining("solo puede crear cuentas de tutor");
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("El administrador NO crea tutores: eso le corresponde a la psicologa")
    void adminNoPuedeCrearTutor() {
        // Arrange
        Usuario admin = usuario("admin@sistema.com", "Admin");
        when(usuarioRepository.buscarPorCorreo("admin@sistema.com")).thenReturn(Optional.of(admin));

        // Act + Assert
        assertThatThrownBy(() -> useCase.crear(
                "Familia", "familia@sistema.com", "Tutor", "admin@sistema.com", "ADMIN"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("los tutores los crea su");
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Correo repetido: se rechaza y no se guarda nada")
    void correoRepetido_seRechaza() {
        // Arrange
        when(usuarioRepository.existePorCorreo("tutor@sistema.com")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> useCase.crear(
                "Tutor Repetido", "tutor@sistema.com", "Tutor", "psicologa@sistema.com", "PSICOLOGO"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("registrado");
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("El administrador crea una psicologa: queda sin psicologa asignada")
    void adminCreaPsicologa_noTieneAsignacion() {
        // Arrange
        Usuario admin = usuario("admin@sistema.com", "Admin");
        when(usuarioRepository.buscarPorCorreo("admin@sistema.com")).thenReturn(Optional.of(admin));
        when(rolRepository.buscarPorNombre("Psicologo")).thenReturn(Optional.of(rol("Psicologo")));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-simulado");
        when(usuarioRepository.guardar(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act
        GestionarUsuariosUseCase.UsuarioCreado creado = useCase.crear(
                "Nueva Psicologa", "nueva@sistema.com", "Psicologo", "admin@sistema.com", "ADMIN");

        // Assert
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).guardar(guardado.capture());
        assertThat(guardado.getValue().getPsicologo()).isNull();
        assertThat(guardado.getValue().getDebeCambiarContrasena()).isTrue();
        assertThat(creado.usuario().rol()).isEqualTo("Psicologo");
    }
}