package com.experienzia.domain.model;

import java.time.LocalDateTime;

public class Certificado {
    private Long id;
    private Long usuarioId;
    private Long eventoId;
    private LocalDateTime fechaGeneracion;
    private String codigoUnico;

    public Certificado() {}

    public Certificado(Long id, Long usuarioId, Long eventoId, LocalDateTime fechaGeneracion, String codigoUnico) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.eventoId = eventoId;
        this.fechaGeneracion = fechaGeneracion;
        this.codigoUnico = codigoUnico;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }
    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
    public String getCodigoUnico() { return codigoUnico; }
    public void setCodigoUnico(String codigoUnico) { this.codigoUnico = codigoUnico; }
}
