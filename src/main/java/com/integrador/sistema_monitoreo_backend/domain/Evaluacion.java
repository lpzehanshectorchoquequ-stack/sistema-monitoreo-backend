package com.integrador.sistema_monitoreo_backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // PENDIENTE (esperando respuestas), POR_REVISAR (respondida) o APROBADA
    @Column(name = "estado", nullable = false)
    private String estado = "APROBADA";

    @Column(name = "cambios_respuestas", nullable = false)
    private Integer cambiosRespuestas = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_respondido_por")
    private Usuario respondidoPor;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_aprobado_por")
    private Usuario aprobadoPor;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @Column(name = "comentario_psicologa")
    private String comentarioPsicologa;

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
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Integer getCambiosRespuestas() { return cambiosRespuestas; }
    public void setCambiosRespuestas(Integer cambiosRespuestas) { this.cambiosRespuestas = cambiosRespuestas; }
    public Usuario getRespondidoPor() { return respondidoPor; }
    public void setRespondidoPor(Usuario respondidoPor) { this.respondidoPor = respondidoPor; }
    public LocalDateTime getFechaRespuesta() { return fechaRespuesta; }
    public void setFechaRespuesta(LocalDateTime fechaRespuesta) { this.fechaRespuesta = fechaRespuesta; }
    public Usuario getAprobadoPor() { return aprobadoPor; }
    public void setAprobadoPor(Usuario aprobadoPor) { this.aprobadoPor = aprobadoPor; }
    public LocalDateTime getFechaAprobacion() { return fechaAprobacion; }
    public void setFechaAprobacion(LocalDateTime fechaAprobacion) { this.fechaAprobacion = fechaAprobacion; }
    public String getComentarioPsicologa() { return comentarioPsicologa; }
    public void setComentarioPsicologa(String comentarioPsicologa) { this.comentarioPsicologa = comentarioPsicologa; }
    public List<Respuesta> getRespuestas() { return respuestas; }
    public List<Conclusion> getConclusiones() { return conclusiones; }
}