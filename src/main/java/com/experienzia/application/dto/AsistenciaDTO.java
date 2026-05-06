package com.experienzia.application.dto;

public class AsistenciaDTO {
    private Long eventoId;
    private Long totalAsistieron;

    public AsistenciaDTO() {}

    public AsistenciaDTO(Long eventoId, Long totalAsistieron) {
        this.eventoId = eventoId;
        this.totalAsistieron = totalAsistieron;
    }

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }
    public Long getTotalAsistieron() { return totalAsistieron; }
    public void setTotalAsistieron(Long totalAsistieron) { this.totalAsistieron = totalAsistieron; }
}
