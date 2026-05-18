package com.experienzia.dto;

import lombok.Data;

import java.util.List;

/**
 * Métricas para el dashboard del organizador (vista "Mis eventos / Mis números").
 */
@Data
public class DashboardOrganizadorDTO {
    private Long organizadorId;
    private long eventosActivos;
    private long eventosPendientes;
    private long eventosCancelados;
    private long eventosTotales;
    private long totalInscritos;
    /** Límite de cupos por evento (regla de negocio; no es capacidad global del salón). */
    private int aforoMaximoPorEvento;
    /** Suma de inscritos/presentes solo en eventos ACTIVO del organizador. */
    private long cuposOcupadosEventosActivos;
    private long asistenciasUltimos30Dias;
    private List<PuntoSerieDTO> serieMensualEventos;
    private List<PuntoSerieDTO> serieMensualInscripciones;
}
