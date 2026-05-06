package com.experienzia.domain.model;

import java.time.LocalDateTime;

public class Notificacion {
    private Long id;
    private Long usuarioId;
    private String mensaje;
    private TipoNotificacion tipo;
    private boolean leida;
    private LocalDateTime fecha;

    public Notificacion() {}

    public Notificacion(Long id, Long usuarioId, String mensaje, TipoNotificacion tipo, boolean leida, LocalDateTime fecha) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.leida = leida;
        this.fecha = fecha;
    }

    public void marcarComoLeida() {
        if (this.leida) {
            throw new IllegalStateException("La notificación ya está marcada como leída.");
        }
        this.leida = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public TipoNotificacion getTipo() { return tipo; }
    public void setTipo(TipoNotificacion tipo) { this.tipo = tipo; }
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
