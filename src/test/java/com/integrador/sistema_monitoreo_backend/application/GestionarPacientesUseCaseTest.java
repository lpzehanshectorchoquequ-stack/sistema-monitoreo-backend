package com.integrador.sistema_monitoreo_backend.application;

import com.integrador.sistema_monitoreo_backend.domain.AccesoDenegadoException;
import com.integrador.sistema_monitoreo_backend.domain.Paciente;
import com.integrador.sistema_monitoreo_backend.domain.PacienteRepository;
import com.integrador.sistema_monitoreo_backend.domain.RecursoNoEncontradoException;
import com.integrador.sistema_monitoreo_backend.domain.Usuario;
import com.integrador.sistema_monitoreo_backend.domain.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestionarPacientesUseCaseTest {

    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private GestionarPacientesUseCase useCase;

    private Usuario psicologa(String correo) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Psicologa " + correo);
        usuario.setCorreo(correo);
        return usuario;
    }

    private Paciente paciente(int id, Usuario psicologo) {
        Paciente paciente = new Paciente();
        paciente.setId(id);
        paciente.setNombre("Juan Perez");
        paciente.setPsicologo(psicologo);
        return paciente;
    }

    @Test
    @DisplayName("La psicologa duena del paciente puede consultarlo")
    void psicologaDuena_puedeConsultarSuPaciente() {
        // Arrange
        Paciente paciente = paciente(1, psicologa("psicologa@sistema.com"));
        when(pacienteRepository.buscarPorId(1)).thenReturn(Optional.of(paciente));

        // Act
        Paciente resultado = useCase.obtener(1, "psicologa@sistema.com", "PSICOLOGO");

        // Assert
        assertThat(resultado).isSameAs(paciente);
    }

    @Test
    @DisplayName("Otra psicologa NO puede consultar un paciente ajeno")
    void otraPsicologa_noPuedeConsultarPacienteAjeno() {
        // Arrange
        Paciente paciente = paciente(1, psicologa("psicologa@sistema.com"));
        when(pacienteRepository.buscarPorId(1)).thenReturn(Optional.of(paciente));

        // Act + Assert
        assertThatThrownBy(() -> useCase.obtener(1, "otra@sistema.com", "PSICOLOGO"))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("El administrador puede consultar cualquier paciente")
    void administrador_puedeConsultarCualquierPaciente() {
        // Arrange
        Paciente paciente = paciente(1, psicologa("psicologa@sistema.com"));
        when(pacienteRepository.buscarPorId(1)).thenReturn(Optional.of(paciente));

        // Act
        Paciente resultado = useCase.obtener(1, "admin@sistema.com", "ADMIN");

        // Assert
        assertThat(resultado).isSameAs(paciente);
    }

    @Test
    @DisplayName("Paciente inexistente: lanza recurso no encontrado")
    void pacienteInexistente_lanzaNoEncontrado() {
        // Arrange
        when(pacienteRepository.buscarPorId(99)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> useCase.obtener(99, "psicologa@sistema.com", "PSICOLOGO"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    @DisplayName("Paciente sin psicologa asignada: una psicologa no puede acceder")
    void pacienteSinPsicologa_noEsAccesibleParaUnaPsicologa() {
        // Arrange
        Paciente huerfano = paciente(2, null);
        when(pacienteRepository.buscarPorId(2)).thenReturn(Optional.of(huerfano));

        // Act + Assert
        assertThatThrownBy(() -> useCase.obtener(2, "psicologa@sistema.com", "PSICOLOGO"))
                .isInstanceOf(AccesoDenegadoException.class);
    }
}