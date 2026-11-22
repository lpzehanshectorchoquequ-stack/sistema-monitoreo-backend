package com.integrador.sistema_monitoreo_backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "respuestas")
@SQLRestriction("eliminado = false")
public class Respuesta extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_respuesta")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evaluacion", nullable = false)
    private Evaluacion evaluacion;

    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "id_pregunta", nullable = false)
    private Pregunta pregunta;

    @Column(name = "valor", nullable = false)
    private Integer valor;

    public Integer getValor() { return valor; }
    public Pregunta getPregunta() { return pregunta; }
    public void setEvaluacion(Evaluacion evaluacion) { this.evaluacion = evaluacion; }
    public void setPregunta(Pregunta pregunta) { this.pregunta = pregunta; }
    public void setValor(Integer valor) { this.valor = valor; }
}