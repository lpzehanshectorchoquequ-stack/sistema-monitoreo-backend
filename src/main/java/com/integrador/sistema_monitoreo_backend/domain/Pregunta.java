package com.integrador.sistema_monitoreo_backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "preguntas")
@SQLRestriction("eliminado = false")
public class Pregunta extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pregunta")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_instrumento", nullable = false)
    private Instrumento instrumento;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "texto", nullable = false)
    private String texto;

    @Column(name = "subescala", nullable = false)
    private String subescala;

    public Integer getId() { return id; }
    public Instrumento getInstrumento() { return instrumento; }
    public void setInstrumento(Instrumento instrumento) { this.instrumento = instrumento; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public String getSubescala() { return subescala; }
    public void setSubescala(String subescala) { this.subescala = subescala; }
}