package com.experienzia.application.dto;

public class ResumenDTO {
    private Long totalUsuarios;
    private Long totalEventos;
    private Long totalInscripciones;

    public ResumenDTO() {}

    public ResumenDTO(Long totalUsuarios, Long totalEventos, Long totalInscripciones) {
        this.totalUsuarios = totalUsuarios;
        this.totalEventos = totalEventos;
        this.totalInscripciones = totalInscripciones;
    }

    public Long getTotalUsuarios() { return totalUsuarios; }
    public void setTotalUsuarios(Long totalUsuarios) { this.totalUsuarios = totalUsuarios; }
    public Long getTotalEventos() { return totalEventos; }
    public void setTotalEventos(Long totalEventos) { this.totalEventos = totalEventos; }
    public Long getTotalInscripciones() { return totalInscripciones; }
    public void setTotalInscripciones(Long totalInscripciones) { this.totalInscripciones = totalInscripciones; }
}
