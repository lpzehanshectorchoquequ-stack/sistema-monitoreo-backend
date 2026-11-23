package com.integrador.sistema_monitoreo_backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "citas")
@SQLRestriction("eliminado = false")
public class Cita extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cita")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_psicologo", nullable = false)
    private Usuario psicologo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_nino", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_solicitante", nullable = false)
    private Usuario solicitante;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "motivo", nullable = false)
    private String motivo;

    @Column(name = "urgente", nullable = false)
    private Boolean urgente = false;

    @Column(name = "estado", nullable = false)
    private String estado;

    @Column(name = "costo", nullable = false)
    private BigDecimal costo = BigDecimal.ZERO;

    @Column(name = "comentario_psicologa")
    private String comentarioPsicologa;

    public Integer getId() { return id; }
    public Usuario getPsicologo() { return psicologo; }
    public void setPsicologo(Usuario psicologo) { this.psicologo = psicologo; }
    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }
    public Usuario getSolicitante() { return solicitante; }
    public void setSolicitante(Usuario solicitante) { this.solicitante = solicitante; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Boolean getUrgente() { return urgente; }
    public void setUrgente(Boolean urgente) { this.urgente = urgente; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public BigDecimal getCosto() { return costo; }
    public void setCosto(BigDecimal costo) { this.costo = costo; }
    public String getComentarioPsicologa() { return comentarioPsicologa; }
    public void setComentarioPsicologa(String comentarioPsicologa) { this.comentarioPsicologa = comentarioPsicologa; }
}