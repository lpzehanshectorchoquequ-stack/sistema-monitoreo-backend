package com.integrador.sistema_monitoreo_backend.domain;

import java.util.Optional;

public interface UsuarioRepository {
    Optional<Usuario> buscarPorCorreo(String correo);
    boolean existePorCorreo(String correo);
    Usuario guardar(Usuario usuario);
}