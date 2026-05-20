package com.experienzia.dto;

import lombok.Data;

@Data
/**
 * Metricas de desempeno de un staff en el evento.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class DesempenoStaffDTO {
    /** Id del usuario con rol STAFF. */
    private Long staffUsuarioId;
    /** Nombre (del evento, usuario, etc.). */
    private String nombre;
    /** Funcion del staff en el evento (check-in, etc.). */
    private String funcion;
    /** Campo check ins registrados. */
    private long checkInsRegistrados;
    /** Campo check outs registrados. */
    private long checkOutsRegistrados;
    /** Campo check ins por q r. */
    private long checkInsPorQR;
    /** Campo check ins manuales. */
    private long checkInsManuales;
}
