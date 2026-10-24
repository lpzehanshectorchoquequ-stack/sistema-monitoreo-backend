package com.integrador.sistema_monitoreo_backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "roles")
@SQLRestriction("eliminado = false")
public class Rol extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Integer id;

    @Column(name = "nombre_rol")
    private String nombreRol;

    // Getters y setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNombreRol() { return nombreRol; }
    public void setNombreRol(String nombreRol) { this.nombreRol = nombreRol; }
}