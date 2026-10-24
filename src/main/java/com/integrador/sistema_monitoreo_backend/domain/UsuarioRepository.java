package com.integrador.sistema_monitoreo_backend.domain;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {
    Optional<Usuario> buscarPorCorreo(String correo);
    Optional<Usuario> buscarPorId(Integer id);
    boolean existePorCorreo(String correo);
    List<Usuario> listarPorRol(String nombreRol);
    Usuario guardar(Usuario usuario);
}