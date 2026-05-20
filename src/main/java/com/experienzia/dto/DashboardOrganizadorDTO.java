package com.experienzia.dto;

import lombok.Data;

import java.util.List;

/**
 * Métricas para el dashboard del organizador (vista "Mis eventos / Mis números").
 */
@Data
/**
 * Metricas para el panel del organizador.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class DashboardOrganizadorDTO {
    /** Id del organizador dueño del evento o staff. */
    private Long organizadorId;
    /** Campo eventos activos. */
    private long eventosActivos;
    /** Campo eventos pendientes. */
    private long eventosPendientes;
    /** Campo eventos cancelados. */
    private long eventosCancelados;
    /** Campo eventos totales. */
    private long eventosTotales;
    /** Campo total inscritos. */
    private long totalInscritos;
    /** Límite de cupos por evento (regla de negocio; no es capacidad global del salón). */
    private int aforoMaximoPorEvento;
    /** Suma de inscritos/presentes solo en eventos ACTIVO del organizador. */
    private long cuposOcupadosEventosActivos;
    /** Campo asistencias ultimos30 dias. */
    private long asistenciasUltimos30Dias;
    /** Campo serie mensual eventos. */
    private List<PuntoSerieDTO> serieMensualEventos;
    /** Campo serie mensual inscripciones. */
    private List<PuntoSerieDTO> serieMensualInscripciones;
}
