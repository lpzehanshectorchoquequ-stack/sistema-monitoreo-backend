package com.integrador.sistema_monitoreo_backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "usuarios")
@SQLRestriction("eliminado = false")
public class Usuario extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "apellido", nullable = false)
    private String apellido;

    @Column(name = "correo", nullable = false, unique = true)
    private String correo;

    @Column(name = "contrasena", nullable = false)
    private String contrasena;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "debe_cambiar_contrasena", nullable = false)
    private Boolean debeCambiarContrasena = false;

    // Solo para tutores: la psicóloga que creó la cuenta
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_psicologo")
    private Usuario psicologo;

    @Column(name = "tarifa_consulta", nullable = false)
    private BigDecimal tarifaConsulta = BigDecimal.ZERO;

    public String getNombreCompleto() {
        String n = nombre == null ? "" : nombre;
        String a = apellido == null ? "" : apellido;
        return (n + " " + a).trim();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public Boolean getDebeCambiarContrasena() { return debeCambiarContrasena; }
    public void setDebeCambiarContrasena(Boolean debeCambiarContrasena) { this.debeCambiarContrasena = debeCambiarContrasena; }
    public Usuario getPsicologo() { return psicologo; }
    public void setPsicologo(Usuario psicologo) { this.psicologo = psicologo; }
    public BigDecimal getTarifaConsulta() { return tarifaConsulta; }
    public void setTarifaConsulta(BigDecimal tarifaConsulta) { this.tarifaConsulta = tarifaConsulta; }
}