package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Motivo para rechazar un evento.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class RechazarEventoDTO {
    /** Motivo de rechazo, cancelacion, etc. */
    private String motivo;
}
