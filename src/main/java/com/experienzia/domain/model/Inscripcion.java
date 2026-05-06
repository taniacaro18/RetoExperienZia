package com.experienzia.domain.model;

import java.time.LocalDateTime;

public class Inscripcion {
    private Long id;
    private Long usuarioId;
    private Long eventoId;
    private LocalDateTime fechaInscripcion;
    private EstadoInscripcion estado;

    public Inscripcion() {}

    public Inscripcion(Long id, Long usuarioId, Long eventoId, LocalDateTime fechaInscripcion, EstadoInscripcion estado) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.eventoId = eventoId;
        this.fechaInscripcion = fechaInscripcion;
        this.estado = estado;
    }

    public void cancelar() {
        if (this.estado == EstadoInscripcion.CANCELADO) {
            throw new IllegalStateException("La inscripción ya está cancelada.");
        }
        if (this.estado == EstadoInscripcion.ASISTIO) {
            throw new IllegalStateException("No se puede cancelar una inscripción si ya se marcó asistencia al evento.");
        }
        this.estado = EstadoInscripcion.CANCELADO;
    }

    public void marcarAsistencia() {
        if (this.estado == EstadoInscripcion.CANCELADO) {
            throw new IllegalStateException("No se puede marcar asistencia en una inscripción cancelada.");
        }
        this.estado = EstadoInscripcion.ASISTIO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }

    public LocalDateTime getFechaInscripcion() { return fechaInscripcion; }
    public void setFechaInscripcion(LocalDateTime fechaInscripcion) { this.fechaInscripcion = fechaInscripcion; }

    public EstadoInscripcion getEstado() { return estado; }
    public void setEstado(EstadoInscripcion estado) { this.estado = estado; }
}
