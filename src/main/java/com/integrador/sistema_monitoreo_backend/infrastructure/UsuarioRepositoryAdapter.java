package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.domain.Usuario;
import com.integrador.sistema_monitoreo_backend.domain.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioRepositoryJPA jpa;

    public UsuarioRepositoryAdapter(UsuarioRepositoryJPA jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return jpa.findByCorreo(correo);
    }

    @Override
    public Optional<Usuario> buscarPorId(Integer id) {
        return jpa.findById(id);
    }

    @Override
    public boolean existePorCorreo(String correo) {
        return jpa.existsByCorreo(correo);
    }

    @Override
    public List<Usuario> listarPorRol(String nombreRol) {
        return jpa.findByRol_NombreRolOrderByNombreAsc(nombreRol);
    }

    @Override
    public List<Usuario> listarTodos() {
        return jpa.findAll(Sort.by("nombre"));
    }

    @Override
    public List<Usuario> listarPorPsicologo(String correoPsicologo) {
        return jpa.findByPsicologo_CorreoOrderByNombreAsc(correoPsicologo);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        return jpa.save(usuario);
    }
}