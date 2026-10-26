package com.integrador.sistema_monitoreo_backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "conclusiones")
@SQLRestriction("eliminado = false")
public class Conclusion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conclusion")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evaluacion", nullable = false)
    private Evaluacion evaluacion;

    @Column(name = "trastorno", nullable = false)
    private String trastorno;

    @Column(name = "nivel", nullable = false)
    private String nivel;

    @Column(name = "explicacion")
    private String explicacion;

    @Column(name = "recomendacion")
    private String recomendacion;

    public void setEvaluacion(Evaluacion evaluacion) { this.evaluacion = evaluacion; }
    public String getTrastorno() { return trastorno; }
    public void setTrastorno(String trastorno) { this.trastorno = trastorno; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public String getExplicacion() { return explicacion; }
    public void setExplicacion(String explicacion) { this.explicacion = explicacion; }
    public String getRecomendacion() { return recomendacion; }
    public void setRecomendacion(String recomendacion) { this.recomendacion = recomendacion; }
}