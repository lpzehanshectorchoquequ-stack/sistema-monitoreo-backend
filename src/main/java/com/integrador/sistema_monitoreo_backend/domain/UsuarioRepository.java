package com.integrador.sistema_monitoreo_backend.domain;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {
    Optional<Usuario> buscarPorCorreo(String correo);
    Optional<Usuario> buscarPorId(Integer id);
    boolean existePorCorreo(String correo);
    List<Usuario> listarPorRol(String nombreRol);
    List<Usuario> listarTodos();
    List<Usuario> listarPorPsicologo(String correoPsicologo);
    Usuario guardar(Usuario usuario);
}