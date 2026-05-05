package com.experienzia.application.service;

import com.experienzia.domain.exception.EventoNoEncontradoException;
import com.experienzia.domain.model.Evento;
import com.experienzia.domain.port.EventoRepository;

public class ActivarEventoPorPagoUseCase {

    private final EventoRepository eventoRepository;

    public ActivarEventoPorPagoUseCase(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    /**
     * HU-014: Activación de evento (luego del pago)
     */
    public Evento activarEvento(Long eventoId) {
        Evento evento = eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new EventoNoEncontradoException("No se encontró el evento con ID: " + eventoId));

        // Valida que esté APROBADO y lo pasa a ACTIVO
        evento.activarPorPago();

        return eventoRepository.guardar(evento);
    }
}
