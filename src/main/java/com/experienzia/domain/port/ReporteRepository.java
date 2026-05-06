package com.experienzia.domain.port;

import com.experienzia.application.dto.AsistenciaDTO;
import com.experienzia.application.dto.EventoPopularDTO;
import com.experienzia.application.dto.ResumenDTO;
import java.util.List;

public interface ReporteRepository {
    List<EventoPopularDTO> obtenerEventosPopulares();
    AsistenciaDTO obtenerAsistenciaPorEvento(Long eventoId);
    List<Long> obtenerUsuariosPorEvento(Long eventoId);
    ResumenDTO obtenerResumenGeneral();
}
