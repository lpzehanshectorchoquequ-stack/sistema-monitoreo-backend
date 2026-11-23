package com.integrador.sistema_monitoreo_backend.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bitacora")
public class Bitacora extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bitacora")
    private Integer id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "usuario", nullable = false)
    private String usuario;

    @Column(name = "rol")
    private String rol;

    @Column(name = "modulo", nullable = false)
    private String modulo;

    @Column(name = "accion", nullable = false)
    private String accion;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "metodo")
    private String metodo;

    @Column(name = "ruta")
    private String ruta;

    @Column(name = "estado_http")
    private Integer estadoHttp;

    @Column(name = "resultado", nullable = false)
    private String resultado;

    @Column(name = "ip")
    private String ip;

    public Integer getId() { return id; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }
    public String getRuta() { return ruta; }
    public void setRuta(String ruta) { this.ruta = ruta; }
    public Integer getEstadoHttp() { return estadoHttp; }
    public void setEstadoHttp(Integer estadoHttp) { this.estadoHttp = estadoHttp; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
}