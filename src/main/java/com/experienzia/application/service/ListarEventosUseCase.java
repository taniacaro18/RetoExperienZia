package com.experienzia.application.service;

import com.experienzia.domain.model.Evento;
import com.experienzia.domain.port.EventoRepository;

import java.util.List;

public class ListarEventosUseCase {

    private final EventoRepository eventoRepository;

    public ListarEventosUseCase(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    /**
     * HU-010: Visualización de eventos (Punto de entrada básico)
     */
    public List<Evento> listarTodos() {
        return eventoRepository.listarTodos();
    }
}
