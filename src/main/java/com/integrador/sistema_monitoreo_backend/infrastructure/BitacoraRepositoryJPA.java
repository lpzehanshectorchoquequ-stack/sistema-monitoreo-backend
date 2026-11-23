package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.domain.Bitacora;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BitacoraRepositoryJPA extends JpaRepository<Bitacora, Integer> {
}