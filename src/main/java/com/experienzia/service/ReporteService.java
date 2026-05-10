package com.experienzia.service;

import com.experienzia.dto.AsistenciaDTO;
import com.experienzia.dto.DashboardAdminDTO;
import com.experienzia.dto.DashboardOrganizadorDTO;
import com.experienzia.dto.EventoPopularDTO;
import com.experienzia.dto.ReporteEventoAvanzadoDTO;
import com.experienzia.dto.ReporteEventoDTO;
import com.experienzia.dto.ResumenDTO;

import java.util.List;

public interface ReporteService {
    List<EventoPopularDTO> obtenerEventosPopulares();
    AsistenciaDTO obtenerAsistenciaPorEvento(Long eventoId);
    List<Long> obtenerUsuariosPorEvento(Long eventoId);
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
