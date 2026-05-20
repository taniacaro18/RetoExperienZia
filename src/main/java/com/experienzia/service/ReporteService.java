package com.experienzia.service;

import com.experienzia.dto.AsistenciaDTO;
import com.experienzia.dto.DashboardAdminDTO;
import com.experienzia.dto.DashboardOrganizadorDTO;
import com.experienzia.dto.EventoPopularDTO;
import com.experienzia.dto.ReporteEventoAvanzadoDTO;
import com.experienzia.dto.ReporteEventoDTO;
import com.experienzia.dto.ResumenDTO;

import java.util.List;

/**
 * Contrato para reportes, estadisticas y dashboards.
 */
public interface ReporteService {

    /** Ranking de eventos con mas inscripciones. */
    List<EventoPopularDTO> obtenerEventosPopulares();
    /** Porcentaje de asistencia de un evento. */
    AsistenciaDTO obtenerAsistenciaPorEvento(Long eventoId);

    /** Ids de usuarios inscritos en un evento. */
    List<Long> obtenerUsuariosPorEvento(Long eventoId);

    /** Totales generales del sistema (usuarios, eventos, etc.). */
    ResumenDTO obtenerResumenGeneral();

    /** HU-023: reporte detallado de un evento (aforo, asistencia, ocupación, lista). */
    ReporteEventoDTO obtenerReporteDetalladoEvento(Long eventoId, Long organizadorId);

    /** Reporte avanzado: curva de ingreso por hora, QR vs manual, desempeño staff. */
    ReporteEventoAvanzadoDTO obtenerReporteAvanzadoEvento(Long eventoId, Long organizadorId);

    /** Métricas para el dashboard del organizador. */
    DashboardOrganizadorDTO obtenerDashboardOrganizador(Long organizadorId);

    /** Métricas globales para el dashboard del administrador. */
    DashboardAdminDTO obtenerDashboardAdmin();
}
