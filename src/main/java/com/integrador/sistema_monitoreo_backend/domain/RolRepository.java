package com.integrador.sistema_monitoreo_backend.domain;

import java.util.List;
import java.util.Optional;

public interface RolRepository {
    List<Rol> obtenerTodos();
    Optional<Rol> buscarPorNombre(String nombreRol);
}