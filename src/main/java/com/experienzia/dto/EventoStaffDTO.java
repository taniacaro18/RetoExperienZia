package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Datos del evento al que un staff está asignado, incluyendo su función específica.
 * Se usa en GET /api/staff/{staffUsuarioId}/eventos para alimentar el panel del staff.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoStaffDTO {
    private Long asignacionId;
    private Long eventoId;
    private String nombreEvento;
    private String descripcion;
    private LocalDateTime fechaEvento;
    private String ubicacion;
    private String tipoEvento;
    private String estadoEvento;
    private String categoria;
    private Integer aforoMaximo;
    private Integer aforoActual;
    private Long organizadorId;
    private String funcion;
}
