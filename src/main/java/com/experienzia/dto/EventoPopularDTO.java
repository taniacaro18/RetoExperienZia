package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Evento con conteo de inscripciones (ranking).
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class EventoPopularDTO {
    /** Id del evento relacionado. */
    private Long eventoId;
    /** Nombre (del evento, usuario, etc.). */
    private String nombre;
    /** Campo total inscritos. */
    private Long totalInscritos;
}
