package com.experienzia.application.service;

import com.experienzia.domain.exception.EventoNoEncontradoException;
import com.experienzia.domain.model.Evento;
import com.experienzia.domain.port.EventoRepository;

public class CancelarEventoUseCase {

    private final EventoRepository eventoRepository;

    public CancelarEventoUseCase(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    /**
     * HU-009: Cancelación de evento
     */
    public Evento cancelarEvento(Long eventoId, Long organizadorId) {
        Evento evento = eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new EventoNoEncontradoException("No se encontró el evento con ID: " + eventoId));

        // Valida internamente que sea el organizador y muta a CANCELADO
        evento.cancelarEvento(organizadorId);

        return eventoRepository.guardar(evento);
    }
}
