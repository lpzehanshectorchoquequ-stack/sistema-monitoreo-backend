package com.integrador.sistema_monitoreo_backend.application;

import com.integrador.sistema_monitoreo_backend.domain.AccesoDenegadoException;
import com.integrador.sistema_monitoreo_backend.domain.Paciente;
import com.integrador.sistema_monitoreo_backend.domain.PacienteRepository;
import com.integrador.sistema_monitoreo_backend.domain.RecursoNoEncontradoException;
import com.integrador.sistema_monitoreo_backend.domain.ReglaNegocioException;
import com.integrador.sistema_monitoreo_backend.domain.Usuario;
import com.integrador.sistema_monitoreo_backend.domain.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class GestionarPacientesUseCase {

    public record Datos(String nombre, LocalDate fechaNacimiento, String motivoConsulta, Integer tutorId) {}

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;

    public GestionarPacientesUseCase(PacienteRepository pacienteRepository,
                                     UsuarioRepository usuarioRepository) {
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<Paciente> listar(String correo, String rol) {
        if ("ADMIN".equals(rol)) {
            return pacienteRepository.listarTodos();
        }
        return pacienteRepository.listarPorPsicologo(correo);
    }

    public List<Paciente> misHijos(String correoTutor) {
        return pacienteRepository.listarPorTutor(correoTutor);
    }

    public List<Usuario> listarTutores() {
        return usuarioRepository.listarPorRol("Tutor");
    }

    public Paciente obtener(Integer id, String correo, String rol) {
        Paciente paciente = pacienteRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado"));

        boolean esAdmin = "ADMIN".equals(rol);
        boolean esSuPsicologo = paciente.getPsicologo() != null
                && paciente.getPsicologo().getCorreo().equals(correo);
        if (!esAdmin && !esSuPsicologo) {
            throw new AccesoDenegadoException();
        }
        return paciente;
    }

    public Paciente crear(Datos datos, String correoPsicologo) {
        Usuario psicologo = usuarioRepository.buscarPorCorreo(correoPsicologo)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado"));
        Paciente paciente = new Paciente();
        aplicar(paciente, datos);
        paciente.setPsicologo(psicologo);
        return pacienteRepository.guardar(paciente);
    }

    public Paciente actualizar(Integer id, Datos datos, String correo, String rol) {
        Paciente paciente = obtener(id, correo, rol);
        aplicar(paciente, datos);
        return pacienteRepository.guardar(paciente);
    }

    public void eliminar(Integer id, String correo, String rol) {
        Paciente paciente = obtener(id, correo, rol);
        paciente.setEliminado(true);
        pacienteRepository.guardar(paciente);
    }

    private void aplicar(Paciente paciente, Datos datos) {
        Usuario tutor = usuarioRepository.buscarPorId(datos.tutorId())
                .filter(u -> "Tutor".equals(u.getRol().getNombreRol()))
                .orElseThrow(() -> new ReglaNegocioException("El tutor indicado no existe"));
        paciente.setNombre(datos.nombre());
        paciente.setFechaNacimiento(datos.fechaNacimiento());
        paciente.setMotivoConsulta(datos.motivoConsulta());
        paciente.setTutor(tutor);
    }
}