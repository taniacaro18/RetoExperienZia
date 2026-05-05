package com.experienzia.application.service;

import com.experienzia.domain.exception.EventoNoEncontradoException;
import com.experienzia.domain.model.Evento;
import com.experienzia.domain.port.EventoRepository;

public class AprobarEventoUseCase {

    private final EventoRepository eventoRepository;

    public AprobarEventoUseCase(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    /**
     * HU-020: Aprobación de solicitudes de evento
     * 
     * @param eventoId El identificador del evento a aprobar
     * @return El evento actualizado con estado APROBADO
     */
    public Evento aprobarEvento(Long eventoId) {
        // 1. Buscar evento y validar existencia
        Evento evento = eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new EventoNoEncontradoException("No se encontró el evento con ID: " + eventoId));

        // 2. Ejecutar la regla de negocio
        // La entidad Evento verificará internamente que su estado actual sea PENDIENTE
        // (y si no lo es, lanzará una excepción).
        evento.aprobarEvento();

        // 3. Persistir los cambios
        return eventoRepository.guardar(evento);
    }
}
