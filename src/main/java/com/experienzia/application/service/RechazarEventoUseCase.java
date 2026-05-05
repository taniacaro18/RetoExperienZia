package com.experienzia.application.service;

import com.experienzia.domain.exception.EventoNoEncontradoException;
import com.experienzia.domain.model.Evento;
import com.experienzia.domain.port.EventoRepository;

public class RechazarEventoUseCase {

    private final EventoRepository eventoRepository;

    public RechazarEventoUseCase(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    /**
     * HU-020: Rechazo de solicitudes de evento
     */
    public Evento rechazarEvento(Long eventoId) {
        Evento evento = eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new EventoNoEncontradoException("No se encontró el evento con ID: " + eventoId));

        evento.rechazarEvento();
        return eventoRepository.guardar(evento);
    }
}
