package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Punto generico para graficos de series.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class PuntoSerieDTO {
    /** Etiqueta del periodo, p. ej. "2026-01" o "Ene". */
    private String periodo;
    /** Valor numérico (cantidad). */
    private long valor;
}
