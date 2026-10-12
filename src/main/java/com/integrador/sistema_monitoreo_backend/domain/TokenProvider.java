package com.integrador.sistema_monitoreo_backend.domain;

public interface TokenProvider {
    String generarToken(Usuario usuario);
}