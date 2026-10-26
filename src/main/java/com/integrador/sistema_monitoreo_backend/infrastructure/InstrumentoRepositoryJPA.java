package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.domain.Instrumento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrumentoRepositoryJPA extends JpaRepository<Instrumento, Integer> {
}