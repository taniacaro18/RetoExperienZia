package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Datos para registrar entrada de un asistente.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class CheckInDTO {
    /** Id del usuario con rol STAFF. */
    private Long staffUsuarioId;
    /** Codigo QR para hacer check-in. */
    private String codigoQR;
    /** Id del evento relacionado. */
    private Long eventoId;
}
