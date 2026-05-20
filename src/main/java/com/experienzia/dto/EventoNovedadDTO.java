package com.experienzia.dto;

import com.experienzia.entity.EstadoNovedadEvento;
import com.experienzia.entity.TipoNovedadEvento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Solicitud de cambio o novedad de un evento.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class EventoNovedadDTO {
    /** Identificador unico del registro. */
    private Long id;
    /** Id del evento relacionado. */
    private Long eventoId;
    /** Campo usuario solicitante id. */
    private Long usuarioSolicitanteId;
    /** Tipo o categoria auxiliar. */
    private TipoNovedadEvento tipo;
    /** Estado actual del registro. */
    private EstadoNovedadEvento estado;
    /** Cuando se hizo la solicitud. */
    private LocalDateTime fechaSolicitud;
    /** Cuando el admin resolvio la solicitud. */
    private LocalDateTime fechaResolucion;
    /** Campo motivo resolucion. */
    private String motivoResolucion;
    /** Detalles extra guardados en JSON. */
    private String detalleJson;
}
