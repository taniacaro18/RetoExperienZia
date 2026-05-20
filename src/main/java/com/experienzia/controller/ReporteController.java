package com.experienzia.controller;

import com.experienzia.dto.AsistenciaDTO;
import com.experienzia.dto.DashboardAdminDTO;
import com.experienzia.dto.DashboardOrganizadorDTO;
import com.experienzia.dto.EventoPopularDTO;
import com.experienzia.dto.ReporteEventoAvanzadoDTO;
import com.experienzia.dto.ReporteEventoDTO;
import com.experienzia.dto.ResumenDTO;
import com.experienzia.service.ReporteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST de reportes y dashboards (estadísticas del sistema).
 * URL base: /api/reportes
 * Lo usan ADMIN (resumen global, dashboard admin) y ORGANIZADOR (reportes de sus eventos).
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    // Devuelve los eventos con más inscripciones (ranking de popularidad).
    @GetMapping("/eventos-populares")
    public ResponseEntity<List<EventoPopularDTO>> eventosPopulares() {
        return ResponseEntity.ok(reporteService.obtenerEventosPopulares());
    }

    // Estadísticas de asistencia de un evento (cuántos entraron, etc.). Devuelve AsistenciaDTO.
    @GetMapping("/asistencia/{eventoId}")
    public ResponseEntity<AsistenciaDTO> asistenciaPorEvento(@PathVariable Long eventoId) {
        return ResponseEntity.ok(reporteService.obtenerAsistenciaPorEvento(eventoId));
    }

    // Lista los ids de usuarios inscritos en un evento. Devuelve lista de Long.
    @GetMapping("/usuarios/{eventoId}")
    public ResponseEntity<List<Long>> usuariosPorEvento(@PathVariable Long eventoId) {
        return ResponseEntity.ok(reporteService.obtenerUsuariosPorEvento(eventoId));
    }

    // Resumen general del sistema (totales de eventos, usuarios, etc.). Devuelve ResumenDTO.
    @GetMapping("/resumen")
    public ResponseEntity<ResumenDTO> resumenGeneral() {
        return ResponseEntity.ok(reporteService.obtenerResumenGeneral());
    }

    // Reporte detallado de un evento (inscripciones, asistencia, etc.). Devuelve ReporteEventoDTO.
    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<ReporteEventoDTO> reporteDetallado(@PathVariable Long eventoId,
                                                             @RequestParam(required = false) Long organizadorId) {
        return ResponseEntity.ok(reporteService.obtenerReporteDetalladoEvento(eventoId, organizadorId));
    }

    // Reporte avanzado con gráficas por hora y desempeño del staff. Devuelve ReporteEventoAvanzadoDTO.
    @GetMapping("/evento/{eventoId}/avanzado")
    public ResponseEntity<ReporteEventoAvanzadoDTO> reporteAvanzado(@PathVariable Long eventoId,
                                                                    @RequestParam(required = false) Long organizadorId) {
        return ResponseEntity.ok(reporteService.obtenerReporteAvanzadoEvento(eventoId, organizadorId));
    }

    // Dashboard con indicadores del organizador. Devuelve DashboardOrganizadorDTO.
    @GetMapping("/dashboard/organizador/{organizadorId}")
    public ResponseEntity<DashboardOrganizadorDTO> dashboardOrganizador(@PathVariable Long organizadorId) {
        return ResponseEntity.ok(reporteService.obtenerDashboardOrganizador(organizadorId));
    }

    // Dashboard global del administrador. Devuelve DashboardAdminDTO.
    @GetMapping("/dashboard/admin")
    public ResponseEntity<DashboardAdminDTO> dashboardAdmin() {
        return ResponseEntity.ok(reporteService.obtenerDashboardAdmin());
    }
}
