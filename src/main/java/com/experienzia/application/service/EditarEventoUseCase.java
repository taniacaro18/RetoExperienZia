package com.experienzia.application.service;

import com.experienzia.domain.exception.EventoNoEncontradoException;
import com.experienzia.domain.model.Evento;
import com.experienzia.domain.port.EventoRepository;
import java.time.LocalDateTime;

public class EditarEventoUseCase {

    private final EventoRepository eventoRepository;

    public EditarEventoUseCase(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    /**
     * HU-008: Edición de evento
     */
    public Evento editarEvento(Long eventoId, Long organizadorId, String nuevoNombre, String nuevaDescripcion, 
                               LocalDateTime nuevaFecha, String nuevaUbicacion, int nuevoAforoMaximo, 
                               double nuevoCosto, String nuevaImagen) {
                               
        Evento evento = eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new EventoNoEncontradoException("No se encontró el evento con ID: " + eventoId));

        // El dominio valida internamente que organizadorId sea el dueño y que el aforoMaximo sea correcto
        evento.actualizarEvento(organizadorId, nuevoNombre, nuevaDescripcion, nuevaFecha, 
                                nuevaUbicacion, nuevoAforoMaximo, nuevoCosto, nuevaImagen);

        return eventoRepository.guardar(evento);
    }
}
