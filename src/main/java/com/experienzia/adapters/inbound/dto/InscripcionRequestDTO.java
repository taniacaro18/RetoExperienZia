package com.experienzia.adapters.inbound.dto;

public class InscripcionRequestDTO {
    private Long usuarioId;
    private Long eventoId;

    public InscripcionRequestDTO() {}

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }
}
