package com.integrador.sistema_monitoreo_backend.domain;

public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException() {
        super("No tienes permiso para acceder a este paciente");
    }
}