package com.integrador.sistema_monitoreo_backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reglas")
@SQLRestriction("eliminado = false")
public class Regla extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regla")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_instrumento", nullable = false)
    private Instrumento instrumento;

    @Column(name = "codigo", nullable = false)
    private String codigo;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "nivel")
    private String nivel;

    @Column(name = "hecho_derivado")
    private String hechoDerivado;

    @Column(name = "recomendacion")
    private String recomendacion;

    @OneToMany(mappedBy = "regla", fetch = FetchType.LAZY)
    private List<CondicionRegla> condiciones = new ArrayList<>();

    public Integer getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
    public String getNivel() { return nivel; }
    public String getHechoDerivado() { return hechoDerivado; }
    public String getRecomendacion() { return recomendacion; }
    public List<CondicionRegla> getCondiciones() { return condiciones; }
}