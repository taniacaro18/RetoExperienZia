package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Aforo en tiempo real de un evento.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class AforoEnVivoDTO {
    /** Id del evento relacionado. */
    private Long eventoId;
    /** Campo nombre evento. */
    private String nombreEvento;
    /** Capacidad maxima de personas. */
    private int aforoMaximo;
    /** Campo inscritos. */
    private long inscritos;
    /** Campo asistencias. */
    private long asistencias;
    /** Campo presentes. */
    private long presentes;
    /** Campo cupos disponibles. */
    private long cuposDisponibles;
    /** Campo porcentaje ocupacion. */
    private double porcentajeOcupacion;
}
