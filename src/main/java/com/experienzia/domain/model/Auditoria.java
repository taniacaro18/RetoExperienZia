package com.experienzia.domain.model;

import java.time.LocalDateTime;

public class Auditoria {
    private Long id;
    private Long usuarioId;
    private String accion;
    private String entidad;
    private Long entidadId;
    private LocalDateTime fecha;

    public Auditoria() {}

    public Auditoria(Long id, Long usuarioId, String accion, String entidad, Long entidadId, LocalDateTime fecha) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }
    public Long getEntidadId() { return entidadId; }
    public void setEntidadId(Long entidadId) { this.entidadId = entidadId; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
