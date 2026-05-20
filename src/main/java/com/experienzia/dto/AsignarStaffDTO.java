package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Datos para asignar staff a un evento.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class AsignarStaffDTO {
    /** Id del organizador dueño del evento o staff. */
    private Long organizadorId;
    /** Id del usuario con rol STAFF. */
    private Long staffUsuarioId;
    /**
     * Función dentro del evento: CHECK_IN_QR, CHECK_IN_MANUAL, REGISTRO_SALIDA o GENERAL.
     * Si no se envía, se asume GENERAL.
     */
    /** Funcion del staff en el evento (check-in, etc.). */
    private String funcion;
}
