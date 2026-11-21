package com.integrador.sistema_monitoreo_backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "instrumentos")
@SQLRestriction("eliminado = false")
public class Instrumento extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_instrumento")
    private Integer id;

    @Column(name = "codigo", nullable = false)
    private String codigo;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "trastorno", nullable = false)
    private String trastorno;

    @Column(name = "quien_responde", nullable = false)
    private String quienResponde;

    @Column(name = "estado_validacion", nullable = false)
    private String estadoValidacion;

    @OneToMany(mappedBy = "instrumento", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("orden ASC")
    private List<Pregunta> preguntas = new ArrayList<>();

    @OneToMany(mappedBy = "instrumento", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Regla> reglas = new ArrayList<>();

    public Integer getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getTrastorno() { return trastorno; }
    public void setTrastorno(String trastorno) { this.trastorno = trastorno; }
    public String getQuienResponde() { return quienResponde; }
    public void setQuienResponde(String quienResponde) { this.quienResponde = quienResponde; }
    public String getEstadoValidacion() { return estadoValidacion; }
    public void setEstadoValidacion(String estadoValidacion) { this.estadoValidacion = estadoValidacion; }
    public List<Pregunta> getPreguntas() { return preguntas; }
    public List<Regla> getReglas() { return reglas; }
}