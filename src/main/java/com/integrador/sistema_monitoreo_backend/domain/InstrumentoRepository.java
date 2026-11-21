package com.integrador.sistema_monitoreo_backend.domain;

import java.util.List;
import java.util.Optional;

public interface InstrumentoRepository {
    List<Instrumento> listarTodos();
    Optional<Instrumento> buscarPorId(Integer id);
    Instrumento guardar(Instrumento instrumento);
}