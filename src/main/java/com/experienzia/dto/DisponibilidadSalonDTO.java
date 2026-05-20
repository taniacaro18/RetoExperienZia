package com.experienzia.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
/**
 * Consulta de ocupacion de un salon.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class DisponibilidadSalonDTO {
    /** Lugar o salon donde es el evento. */
    private String ubicacion;
    /** Fecha inicio del rango consultado. */
    private LocalDateTime desde;
    /** Fecha fin del rango consultado. */
    private LocalDateTime hasta;
    /** Si se envió propuesta de horario, indica si no choca con ninguna ocupación. */
    private Boolean propuestaDisponible;
    /** Mensaje sobre la disponibilidad propuesta. */
    private String mensajePropuesta;
    private List<FranjaOcupacionSalonDTO> ocupaciones = new ArrayList<>();
}
