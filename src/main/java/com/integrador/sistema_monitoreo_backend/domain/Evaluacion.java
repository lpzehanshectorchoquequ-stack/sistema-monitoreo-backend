package com.integrador.sistema_monitoreo_backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluaciones")
@SQLRestriction("eliminado = false")
public class Evaluacion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_nino", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_psicologo", nullable = false)
    private Usuario psicologo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_instrumento", nullable = false)
    private Instrumento instrumento;

    @Column(name = "fecha_evaluacion", nullable = false)
    private LocalDate fechaEvaluacion;

    @Column(name = "observaciones")
    private String observaciones;

    @OneToMany(mappedBy = "evaluacion", cascade = CascadeType.ALL)
    private List<Respuesta> respuestas = new ArrayList<>();

    @OneToMany(mappedBy = "evaluacion", cascade = CascadeType.ALL)
    private List<Conclusion> conclusiones = new ArrayList<>();

    public Integer getId() { return id; }
    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }
    public Usuario getPsicologo() { return psicologo; }
    public void setPsicologo(Usuario psicologo) { this.psicologo = psicologo; }
    public Instrumento getInstrumento() { return instrumento; }
    public void setInstrumento(Instrumento instrumento) { this.instrumento = instrumento; }
    public LocalDate getFechaEvaluacion() { return fechaEvaluacion; }
    public void setFechaEvaluacion(LocalDate fechaEvaluacion) { this.fechaEvaluacion = fechaEvaluacion; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public List<Respuesta> getRespuestas() { return respuestas; }
    public List<Conclusion> getConclusiones() { return conclusiones; }
}