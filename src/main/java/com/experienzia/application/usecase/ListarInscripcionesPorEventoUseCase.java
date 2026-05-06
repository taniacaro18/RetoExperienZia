package com.experienzia.application.usecase;

import com.experienzia.domain.model.Inscripcion;
import com.experienzia.domain.port.InscripcionRepository;
import java.util.List;

public class ListarInscripcionesPorEventoUseCase {

    private final InscripcionRepository inscripcionRepository;

    public ListarInscripcionesPorEventoUseCase(InscripcionRepository inscripcionRepository) {
        this.inscripcionRepository = inscripcionRepository;
    }

    public List<Inscripcion> ejecutar(Long eventoId) {
        return inscripcionRepository.listarPorEvento(eventoId);
    }
}
