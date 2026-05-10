package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignarStaffDTO {
    private Long organizadorId;
    private Long staffUsuarioId;
    /**
     * Función dentro del evento: CHECK_IN_QR, CHECK_IN_MANUAL, REGISTRO_SALIDA o GENERAL.
     * Si no se envía, se asume GENERAL.
     */
    private String funcion;
}
