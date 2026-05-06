package com.experienzia.application.usecase;

import com.experienzia.application.dto.AsistenciaDTO;
import com.experienzia.application.dto.EventoPopularDTO;
import com.experienzia.application.dto.ResumenDTO;
import com.experienzia.domain.port.ReporteRepository;

import java.util.List;

public class ReporteUseCase {

    private final ReporteRepository reporteRepository;

    public ReporteUseCase(ReporteRepository reporteRepository) {
        this.reporteRepository = reporteRepository;
    }

    public List<EventoPopularDTO> obtenerEventosPopulares() {
        return reporteRepository.obtenerEventosPopulares();
    }

    public AsistenciaDTO obtenerAsistenciaPorEvento(Long eventoId) {
        return reporteRepository.obtenerAsistenciaPorEvento(eventoId);
    }

    public List<Long> obtenerUsuariosPorEvento(Long eventoId) {
        return reporteRepository.obtenerUsuariosPorEvento(eventoId);
    }

    public ResumenDTO obtenerResumenGeneral() {
        return reporteRepository.obtenerResumenGeneral();
    }
}
