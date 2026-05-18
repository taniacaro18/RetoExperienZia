package com.experienzia.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DisponibilidadSalonDTO {
    private String ubicacion;
    private LocalDateTime desde;
    private LocalDateTime hasta;
    /** Si se envió propuesta de horario, indica si no choca con ninguna ocupación. */
    private Boolean propuestaDisponible;
    private String mensajePropuesta;
    private List<FranjaOcupacionSalonDTO> ocupaciones = new ArrayList<>();
}
