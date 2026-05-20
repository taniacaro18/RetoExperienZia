package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Un punto de la curva de ingreso por hora.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class CurvaIngresoPuntoDTO {
    /** Hora del día (0-23) relativa a la fecha del evento. */
    private int hora;
    /** Cantidad de ingresos (check-in) en esa hora. */
    private long ingresos;
    /** Cantidad de salidas (check-out) en esa hora. */
    private long salidas;
}
