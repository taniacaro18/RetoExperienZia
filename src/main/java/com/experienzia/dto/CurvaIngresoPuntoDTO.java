package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurvaIngresoPuntoDTO {
    /** Hora del día (0-23) relativa a la fecha del evento. */
    private int hora;
    /** Cantidad de ingresos (check-in) en esa hora. */
    private long ingresos;
    /** Cantidad de salidas (check-out) en esa hora. */
    private long salidas;
}
