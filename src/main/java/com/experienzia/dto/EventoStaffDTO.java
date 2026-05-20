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
/**
 * Evento visto desde el panel del staff.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class EventoStaffDTO {
    /** Campo asignacion id. */
    private Long asignacionId;
    /** Id del evento relacionado. */
    private Long eventoId;
    /** Campo nombre evento. */
    private String nombreEvento;
    /** Descripcion o detalle del evento. */
    private String descripcion;
    /** Fecha en que ocurrio el evento. */
    private LocalDateTime fechaEvento;
    /** Lugar o salon donde es el evento. */
    private String ubicacion;
    /** Si el evento es PUBLICO o PRIVADO. */
    private String tipoEvento;
    /** Campo estado evento. */
    private String estadoEvento;
    /** Categoria del evento (conferencia, taller, etc.). */
    private String categoria;
    /** Capacidad maxima de personas. */
    private Integer aforoMaximo;
    /** Cuantas personas estan inscritas ahora. */
    private Integer aforoActual;
    /** Id del organizador dueño del evento o staff. */
    private Long organizadorId;
    /** Funcion del staff en el evento (check-in, etc.). */
    private String funcion;
}
