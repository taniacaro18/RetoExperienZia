package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AforoEnVivoDTO {
    private Long eventoId;
    private String nombreEvento;
    private int aforoMaximo;
    private long inscritos;
    private long asistencias;
    private long presentes;
    private long cuposDisponibles;
    private double porcentajeOcupacion;
}
