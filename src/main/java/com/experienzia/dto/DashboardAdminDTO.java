package com.experienzia.dto;

import lombok.Data;

import java.util.List;

/**
 * Métricas globales para el dashboard del administrador.
 */
@Data
/**
 * Metricas para el panel del administrador.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class DashboardAdminDTO {
    /** Campo eventos activos. */
    private long eventosActivos;
    /** Campo eventos pendientes. */
    private long eventosPendientes;
    /** Campo eventos cancelados. */
    private long eventosCancelados;
    /** Campo eventos totales. */
    private long eventosTotales;
    /** Campo usuarios totales. */
    private long usuariosTotales;
    /** Campo usuarios activos. */
    private long usuariosActivos;
    /** Campo usuarios pendientes. */
    private long usuariosPendientes;
    /** Campo organizadores activos. */
    private long organizadoresActivos;
    /** Campo asistentes totales. */
    private long asistentesTotales;
    /** Campo staff totales. */
    private long staffTotales;
    /** Campo inscripciones totales. */
    private long inscripcionesTotales;
    /** Campo serie mensual eventos. */
    private List<PuntoSerieDTO> serieMensualEventos;
    /** Campo serie mensual usuarios. */
    private List<PuntoSerieDTO> serieMensualUsuarios;
}
