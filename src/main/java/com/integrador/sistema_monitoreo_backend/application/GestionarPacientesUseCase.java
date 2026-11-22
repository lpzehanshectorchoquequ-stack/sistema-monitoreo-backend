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
import java.time.Period;
import java.util.List;

@Service
public class GestionarPacientesUseCase {

    public record Datos(String nombre, String apellido, LocalDate fechaNacimiento, String motivoConsulta,
                        Integer tutorId, String genero, String fortalezas, String dificultades) {}

    public record DatosHijo(String nombre, String apellido, LocalDate fechaNacimiento, String genero,
                            String motivoConsulta) {}

    private static final int EDAD_MINIMA = 6;
    private static final int EDAD_MAXIMA = 12;

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

    public List<Usuario> listarPsicologos() {
        return usuarioRepository.listarPorRol("Psicologo");
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

    public Paciente obtenerParaLectura(Integer id, String correo, String rol) {
        Paciente paciente = pacienteRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado"));

        boolean esAdmin = "ADMIN".equals(rol);
        boolean esSuPsicologo = paciente.getPsicologo() != null
                && paciente.getPsicologo().getCorreo().equals(correo);
        boolean esSuTutor = paciente.getTutor() != null
                && paciente.getTutor().getCorreo().equals(correo);
        if (!esAdmin && !esSuPsicologo && !esSuTutor) {
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

    public Paciente registrarHijo(DatosHijo datos, String correoTutor) {
        validarFecha(datos.fechaNacimiento());
        Usuario tutor = usuarioRepository.buscarPorCorreo(correoTutor)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado"));
        Usuario psicologo = tutor.getPsicologo();
        if (psicologo == null) {
            throw new ReglaNegocioException(
                    "Tu cuenta aún no tiene una psicóloga asignada. Comunícate con tu psicóloga.");
        }

        Paciente paciente = new Paciente();
        paciente.setNombre(datos.nombre().trim());
        paciente.setApellido(datos.apellido().trim());
        paciente.setFechaNacimiento(datos.fechaNacimiento());
        paciente.setGenero(datos.genero());
        paciente.setMotivoConsulta(datos.motivoConsulta());
        paciente.setTutor(tutor);
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
        validarFecha(datos.fechaNacimiento());
        Usuario tutor = usuarioRepository.buscarPorId(datos.tutorId())
                .filter(u -> "Tutor".equals(u.getRol().getNombreRol()))
                .orElseThrow(() -> new ReglaNegocioException("El tutor indicado no existe"));
        paciente.setNombre(datos.nombre().trim());
        paciente.setApellido(datos.apellido().trim());
        paciente.setFechaNacimiento(datos.fechaNacimiento());
        paciente.setMotivoConsulta(datos.motivoConsulta());
        paciente.setTutor(tutor);
        paciente.setGenero(datos.genero() == null || datos.genero().isBlank() ? null : datos.genero());
        paciente.setFortalezas(datos.fortalezas());
        paciente.setDificultades(datos.dificultades());
    }

    private void validarFecha(LocalDate fecha) {
        LocalDate hoy = LocalDate.now();
        if (fecha.isAfter(hoy)) {
            throw new ReglaNegocioException("La fecha de nacimiento no puede ser futura");
        }
        int edad = Period.between(fecha, hoy).getYears();
        if (edad < EDAD_MINIMA || edad > EDAD_MAXIMA) {
            throw new ReglaNegocioException(
                    "La edad del niño o niña debe estar entre " + EDAD_MINIMA + " y " + EDAD_MAXIMA + " años");
        }
    }
}