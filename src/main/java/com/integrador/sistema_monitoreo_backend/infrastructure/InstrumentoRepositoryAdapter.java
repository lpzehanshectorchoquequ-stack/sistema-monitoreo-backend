package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.domain.Instrumento;
import com.integrador.sistema_monitoreo_backend.domain.InstrumentoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class InstrumentoRepositoryAdapter implements InstrumentoRepository {

    private final InstrumentoRepositoryJPA jpa;

    public InstrumentoRepositoryAdapter(InstrumentoRepositoryJPA jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Instrumento> listarTodos() {
        return jpa.findAll(Sort.by("id"));
    }

    @Override
    public Optional<Instrumento> buscarPorId(Integer id) {
        return jpa.findById(id);
    }

    @Override
    public Instrumento guardar(Instrumento instrumento) {
        // saveAndFlush asegura que las preguntas nuevas ya tengan su id al responder
        return jpa.saveAndFlush(instrumento);
    }
}