package com.integrador.sistema_monitoreo_backend.infrastructure;

import com.integrador.sistema_monitoreo_backend.domain.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepositoryJPA extends JpaRepository<Rol, Integer> {
}