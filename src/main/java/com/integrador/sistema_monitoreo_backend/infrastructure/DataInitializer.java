package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.domain.Rol;
import com.integrador.sistema_monitoreo_backend.domain.RolRepository;
import com.integrador.sistema_monitoreo_backend.domain.Usuario;
import com.integrador.sistema_monitoreo_backend.domain.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminCorreo;
    private final String adminContrasena;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.admin.correo}") String adminCorreo,
                           @Value("${app.admin.contrasena}") String adminContrasena) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminCorreo = adminCorreo;
        this.adminContrasena = adminContrasena;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.existePorCorreo(adminCorreo)) {
            return;
        }
        Rol rolAdmin = rolRepository.buscarPorNombre("Admin").orElseThrow();
        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setApellido("del Sistema");
        admin.setCorreo(adminCorreo);
        admin.setContrasena(passwordEncoder.encode(adminContrasena));
        admin.setRol(rolAdmin);
        usuarioRepository.guardar(admin);
    }
}