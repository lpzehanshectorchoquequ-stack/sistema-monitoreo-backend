package com.integrador.sistema_monitoreo_backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "condiciones_regla")
@SQLRestriction("eliminado = false")
public class CondicionRegla extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_condicion")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_regla", nullable = false)
    private Regla regla;

    @Column(name = "hecho", nullable = false)
    private String hecho;

    @Column(name = "operador", nullable = false)
    private String operador;

    @Column(name = "valor", nullable = false)
    private BigDecimal valor;

    public Integer getId() { return id; }
    public Regla getRegla() { return regla; }
    public void setRegla(Regla regla) { this.regla = regla; }
    public String getHecho() { return hecho; }
    public void setHecho(String hecho) { this.hecho = hecho; }
    public String getOperador() { return operador; }
    public void setOperador(String operador) { this.operador = operador; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
}