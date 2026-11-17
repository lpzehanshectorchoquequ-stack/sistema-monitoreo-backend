package com.integrador.sistema_monitoreo_backend.application;

import com.integrador.sistema_monitoreo_backend.domain.AccesoDenegadoException;
import com.integrador.sistema_monitoreo_backend.domain.RecursoNoEncontradoException;
import com.integrador.sistema_monitoreo_backend.domain.ReglaNegocioException;
import com.integrador.sistema_monitoreo_backend.domain.Rol;
import com.integrador.sistema_monitoreo_backend.domain.RolRepository;
import com.integrador.sistema_monitoreo_backend.domain.Usuario;
import com.integrador.sistema_monitoreo_backend.domain.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
public class GestionarUsuariosUseCase {

    public record UsuarioVista(Integer id, String nombre, String correo, String rol, boolean activo,
                               boolean pendienteCambio, String psicologoNombre) {}

    public record UsuarioCreado(UsuarioVista usuario, String contrasenaTemporal) {}

    // Sin caracteres que se confunden (0/O, 1/I)
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom azar = new SecureRandom();

    public GestionarUsuariosUseCase(UsuarioRepository usuarioRepository,
                                    RolRepository rolRepository,
                                    PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioVista> listar(String correo, String rol) {
        List<Usuario> usuarios = "ADMIN".equals(rol)
                ? usuarioRepository.listarTodos()
                : usuarioRepository.listarPorPsicologo(correo);
        return usuarios.stream().map(this::aVista).toList();
    }

    public UsuarioCreado crear(String nombre, String correo, String nombreRol,
                               String correoCreador, String rolCreador) {
        if (usuarioRepository.existePorCorreo(correo)) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }
        Usuario creador = usuarioRepository.buscarPorCorreo(correoCreador)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado"));

        if ("PSICOLOGO".equals(rolCreador)) {
            if (!"Tutor".equals(nombreRol)) {
                throw new AccesoDenegadoException("Una psicóloga solo puede crear cuentas de tutor");
            }
        } else if (!"Admin".equals(nombreRol) && !"Psicologo".equals(nombreRol)) {
            throw new ReglaNegocioException(
                    "El administrador crea cuentas de administrador o psicóloga; los tutores los crea su psicóloga");
        }
        Rol rol = rolRepository.buscarPorNombre(nombreRol)
                .orElseThrow(() -> new ReglaNegocioException("El rol indicado no existe"));

        String temporal = generarContrasenaTemporal();
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setCorreo(correo);
        usuario.setRol(rol);
        usuario.setContrasena(passwordEncoder.encode(temporal));
        usuario.setDebeCambiarContrasena(true);
        if ("Tutor".equals(nombreRol)) {
            usuario.setPsicologo(creador);
        }
        return new UsuarioCreado(aVista(usuarioRepository.guardar(usuario)), temporal);
    }

    public UsuarioCreado restablecerContrasena(Integer id, String correo, String rol) {
        Usuario objetivo = obtenerAdministrable(id, correo, rol);
        String temporal = generarContrasenaTemporal();
        objetivo.setContrasena(passwordEncoder.encode(temporal));
        objetivo.setDebeCambiarContrasena(true);
        return new UsuarioCreado(aVista(usuarioRepository.guardar(objetivo)), temporal);
    }

    public UsuarioVista cambiarEstado(Integer id, boolean activo, String correo, String rol) {
        Usuario objetivo = obtenerAdministrable(id, correo, rol);
        if (objetivo.getCorreo().equals(correo)) {
            throw new ReglaNegocioException("No puedes cambiar el estado de tu propia cuenta");
        }
        objetivo.setActivo(activo);
        return aVista(usuarioRepository.guardar(objetivo));
    }

    // El Admin administra a todos; la psicóloga solo a sus tutores
    private Usuario obtenerAdministrable(Integer id, String correo, String rol) {
        Usuario objetivo = usuarioRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if ("ADMIN".equals(rol)) {
            return objetivo;
        }
        boolean esSuTutor = "Tutor".equals(objetivo.getRol().getNombreRol())
                && objetivo.getPsicologo() != null
                && objetivo.getPsicologo().getCorreo().equals(correo);
        if (!esSuTutor) {
            throw new AccesoDenegadoException("Solo puedes administrar las cuentas de tus tutores");
        }
        return objetivo;
    }

    private String generarContrasenaTemporal() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    private UsuarioVista aVista(Usuario u) {
        return new UsuarioVista(u.getId(), u.getNombre(), u.getCorreo(), u.getRol().getNombreRol(),
                Boolean.TRUE.equals(u.getActivo()),
                Boolean.TRUE.equals(u.getDebeCambiarContrasena()),
                u.getPsicologo() != null ? u.getPsicologo().getNombre() : null);
    }
}