package com.experienzia.dto;

import com.experienzia.entity.EstadoEvento;
import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * Una franja horaria ocupada en el salon.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class FranjaOcupacionSalonDTO {
    /** Id del evento relacionado. */
    private Long eventoId;
    /** Campo nombre evento. */
    private String nombreEvento;
    /** Estado actual del registro. */
    private EstadoEvento estado;
    /** Campo inicio. */
    private LocalDateTime inicio;
    /** Campo fin. */
    private LocalDateTime fin;
    /** Campo nombre organizador. */
    private String nombreOrganizador;
}
