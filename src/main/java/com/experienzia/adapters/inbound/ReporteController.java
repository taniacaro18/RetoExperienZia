package com.experienzia.adapters.inbound;

import com.experienzia.application.dto.AsistenciaDTO;
import com.experienzia.application.dto.EventoPopularDTO;
import com.experienzia.application.dto.ResumenDTO;
import com.experienzia.application.usecase.ReporteUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteUseCase reporteUseCase;

    public ReporteController(ReporteUseCase reporteUseCase) {
        this.reporteUseCase = reporteUseCase;
    }

    @GetMapping("/eventos-populares")
    public ResponseEntity<List<EventoPopularDTO>> obtenerEventosPopulares() {
        return ResponseEntity.ok(reporteUseCase.obtenerEventosPopulares());
    }

    @GetMapping("/asistencia/{eventoId}")
    public ResponseEntity<AsistenciaDTO> obtenerAsistenciaPorEvento(@PathVariable Long eventoId) {
        return ResponseEntity.ok(reporteUseCase.obtenerAsistenciaPorEvento(eventoId));
    }

    @GetMapping("/usuarios/{eventoId}")
    public ResponseEntity<List<Long>> obtenerUsuariosPorEvento(@PathVariable Long eventoId) {
        return ResponseEntity.ok(reporteUseCase.obtenerUsuariosPorEvento(eventoId));
    }

    @GetMapping("/resumen")
    public ResponseEntity<ResumenDTO> obtenerResumenGeneral() {
        return ResponseEntity.ok(reporteUseCase.obtenerResumenGeneral());
    }
}
