package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Reporte detallado de un evento.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class ReporteEventoDTO {
    /** Id del evento relacionado. */
    private Long eventoId;
    /** Campo nombre evento. */
    private String nombreEvento;
    /** Campo estado evento. */
    private String estadoEvento;
    /** Fecha en que ocurrio el evento. */
    private LocalDateTime fechaEvento;
    /** Duracion del evento en horas. */
    private Integer duracionHoras;
    /** Capacidad maxima de personas. */
    private int aforoMaximo;
    /** Campo inscritos. */
    private long inscritos;
    /** Campo asistencias reales. */
    private long asistenciasReales;
    /** Campo asistentes actualmente en sala. */
    private long asistentesActualmenteEnSala;
    /** Campo porcentaje ocupacion. */
    private double porcentajeOcupacion;
    /** Campo porcentaje asistencia sobre inscritos. */
    private double porcentajeAsistenciaSobreInscritos;
    /** Lista de asistentes. */
    private List<AsistenteEventoDTO> asistentes;
}
