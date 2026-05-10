package com.experienzia.dto;

import lombok.Data;

import java.util.List;

/**
 * Métricas globales para el dashboard del administrador.
 */
@Data
public class DashboardAdminDTO {
    private long eventosActivos;
    private long eventosPendientes;
    private long eventosCancelados;
    private long eventosTotales;
    private long usuariosTotales;
    private long usuariosActivos;
    private long usuariosPendientes;
    private long organizadoresActivos;
    private long asistentesTotales;
    private long staffTotales;
    private long inscripcionesTotales;
    private List<PuntoSerieDTO> serieMensualEventos;
    private List<PuntoSerieDTO> serieMensualUsuarios;
}
