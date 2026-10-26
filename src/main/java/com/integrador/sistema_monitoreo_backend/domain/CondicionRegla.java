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

    public String getHecho() { return hecho; }
    public String getOperador() { return operador; }
    public BigDecimal getValor() { return valor; }
}