package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Motivo para rechazar un pago.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class RechazarPagoDTO {
    /** Motivo de rechazo, cancelacion, etc. */
    private String motivo;
    /** Campo aprobador id. */
    private Long aprobadorId;
}
