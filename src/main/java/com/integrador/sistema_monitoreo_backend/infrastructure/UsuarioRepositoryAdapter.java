package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.domain.Usuario;
import com.integrador.sistema_monitoreo_backend.domain.UsuarioRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioRepositoryJPA usuarioRepositoryJPA;

    public UsuarioRepositoryAdapter(UsuarioRepositoryJPA usuarioRepositoryJPA) {
        this.usuarioRepositoryJPA = usuarioRepositoryJPA;
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioRepositoryJPA.findByCorreo(correo);
    }

    @Override
    public boolean existePorCorreo(String correo) {
        return usuarioRepositoryJPA.existsByCorreo(correo);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        return usuarioRepositoryJPA.save(usuario);
    }
}