package com.integrador.sistema_monitoreo_backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalTime;

@Entity
@Table(name = "horarios_atencion")
@SQLRestriction("eliminado = false")
public class HorarioAtencion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_psicologo", nullable = false)
    private Usuario psicologo;

    // 1 = lunes ... 7 = domingo
    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    public Integer getId() { return id; }
    public Usuario getPsicologo() { return psicologo; }
    public void setPsicologo(Usuario psicologo) { this.psicologo = psicologo; }
    public Integer getDiaSemana() { return diaSemana; }
    public void setDiaSemana(Integer diaSemana) { this.diaSemana = diaSemana; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
}