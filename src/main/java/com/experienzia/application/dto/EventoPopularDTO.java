package com.experienzia.application.dto;

public class EventoPopularDTO {
    private Long eventoId;
    private String nombre;
    private Long totalInscritos;

    public EventoPopularDTO() {}

    public EventoPopularDTO(Long eventoId, String nombre, Long totalInscritos) {
        this.eventoId = eventoId;
        this.nombre = nombre;
        this.totalInscritos = totalInscritos;
    }

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Long getTotalInscritos() { return totalInscritos; }
    public void setTotalInscritos(Long totalInscritos) { this.totalInscritos = totalInscritos; }
}
