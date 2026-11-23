package com.integrador.sistema_monitoreo_backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ninos")
@SQLRestriction("eliminado = false")
public class Paciente extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nino")
    private Integer id;

    // Tutor principal
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tutor")
    private Usuario tutor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_psicologo")
    private Usuario psicologo;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "apellido", nullable = false)
    private String apellido;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "motivo_consulta")
    private String motivoConsulta;

    @Column(name = "genero")
    private String genero;

    @Column(name = "fortalezas")
    private String fortalezas;

    @Column(name = "dificultades")
    private String dificultades;

    // Tutores adicionales (además del principal)
    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL)
    private List<VinculoTutor> vinculos = new ArrayList<>();

    public String getNombreCompleto() {
        String n = nombre == null ? "" : nombre;
        String a = apellido == null ? "" : apellido;
        return (n + " " + a).trim();
    }

    // ¿Esta cuenta es tutor de este niño? (principal o vinculado)
    public boolean esTutor(String correo) {
        if (tutor != null && tutor.getCorreo().equals(correo)) {
            return true;
        }
        return vinculos.stream()
                .anyMatch(v -> !Boolean.TRUE.equals(v.getEliminado()) && v.getTutor().getCorreo().equals(correo));
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Usuario getTutor() { return tutor; }
    public void setTutor(Usuario tutor) { this.tutor = tutor; }
    public Usuario getPsicologo() { return psicologo; }
    public void setPsicologo(Usuario psicologo) { this.psicologo = psicologo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getMotivoConsulta() { return motivoConsulta; }
    public void setMotivoConsulta(String motivoConsulta) { this.motivoConsulta = motivoConsulta; }
    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }
    public String getFortalezas() { return fortalezas; }
    public void setFortalezas(String fortalezas) { this.fortalezas = fortalezas; }
    public String getDificultades() { return dificultades; }
    public void setDificultades(String dificultades) { this.dificultades = dificultades; }
    public List<VinculoTutor> getVinculos() { return vinculos; }
}