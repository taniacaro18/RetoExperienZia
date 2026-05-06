package com.experienzia.domain.model;

import java.time.LocalDateTime;

public class Pago {
    private Long id;
    private Long inscripcionId;
    private String comprobanteUrl;
    private EstadoPago estado;
    private LocalDateTime fecha;

    public Pago() {}

    public Pago(Long id, Long inscripcionId, String comprobanteUrl, EstadoPago estado, LocalDateTime fecha) {
        this.id = id;
        this.inscripcionId = inscripcionId;
        this.comprobanteUrl = comprobanteUrl;
        this.estado = estado;
        this.fecha = fecha;
    }

    public void aprobar() {
        if (this.estado != EstadoPago.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden aprobar pagos PENDIENTES.");
        }
        this.estado = EstadoPago.APROBADO;
    }

    public void rechazar() {
        if (this.estado != EstadoPago.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden rechazar pagos PENDIENTES.");
        }
        this.estado = EstadoPago.RECHAZADO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getInscripcionId() { return inscripcionId; }
    public void setInscripcionId(Long inscripcionId) { this.inscripcionId = inscripcionId; }
    public String getComprobanteUrl() { return comprobanteUrl; }
    public void setComprobanteUrl(String comprobanteUrl) { this.comprobanteUrl = comprobanteUrl; }
    public EstadoPago getEstado() { return estado; }
    public void setEstado(EstadoPago estado) { this.estado = estado; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
