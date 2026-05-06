package com.experienzia.application.usecase;

import com.experienzia.domain.model.Evento;
import com.experienzia.domain.model.Inscripcion;
import com.experienzia.domain.port.EventoRepository;
import com.experienzia.domain.port.InscripcionRepository;
import java.util.Optional;

public class CancelarInscripcionUseCase {

    private final InscripcionRepository inscripcionRepository;
    private final EventoRepository eventoRepository;

    public CancelarInscripcionUseCase(InscripcionRepository inscripcionRepository, EventoRepository eventoRepository) {
        this.inscripcionRepository = inscripcionRepository;
        this.eventoRepository = eventoRepository;
    }

    public Inscripcion ejecutar(Long id) {
        Optional<Inscripcion> inscripcionOpt = inscripcionRepository.buscarPorId(id);
        
        if (inscripcionOpt.isEmpty()) {
            throw new IllegalArgumentException("Inscripción no encontrada con ID: " + id);
        }

        Inscripcion inscripcion = inscripcionOpt.get();
        inscripcion.cancelar(); // Regla de negocio: pasa a CANCELADO

        Evento evento = eventoRepository.buscarPorId(inscripcion.getEventoId())
                .orElseThrow(() -> new IllegalStateException("El evento asociado a la inscripción no existe."));

        evento.disminuirAforo();
        eventoRepository.guardar(evento);

        return inscripcionRepository.guardar(inscripcion);
    }
}
