package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Motivo para cancelar un evento.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class CancelarEventoDTO {
    /** Id del organizador dueño del evento o staff. */
    private Long organizadorId;
    /** Motivo de rechazo, cancelacion, etc. */
    private String motivo;
}
