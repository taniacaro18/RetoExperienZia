package com.experienzia.application.usecase;

import com.experienzia.domain.exception.EventoNoDisponibleException;
import com.experienzia.domain.exception.EventoSinCupoException;
import com.experienzia.domain.exception.InscripcionDuplicadaException;
import com.experienzia.domain.model.EstadoEvento;
import com.experienzia.domain.model.EstadoInscripcion;
import com.experienzia.domain.model.Evento;
import com.experienzia.domain.model.Inscripcion;
import com.experienzia.domain.port.EventoRepository;
import com.experienzia.domain.port.InscripcionRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public class CrearInscripcionUseCase {

    private final InscripcionRepository inscripcionRepository;
    private final EventoRepository eventoRepository;

    public CrearInscripcionUseCase(InscripcionRepository inscripcionRepository, EventoRepository eventoRepository) {
        this.inscripcionRepository = inscripcionRepository;
        this.eventoRepository = eventoRepository;
    }

    public Inscripcion ejecutar(Long usuarioId, Long eventoId) {
        Evento evento = eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new EventoNoDisponibleException("El evento no existe."));

        if (evento.getEstado() == EstadoEvento.CANCELADO) {
            throw new EventoNoDisponibleException("No se puede inscribir a un evento CANCELADO.");
        }

        if (evento.getEstado() != EstadoEvento.ACTIVO) {
            throw new EventoNoDisponibleException("No se puede inscribir a un evento que no está ACTIVO.");
        }

        if (!evento.tieneCupoDisponible()) {
            throw new EventoSinCupoException("El evento ya no tiene cupo disponible.");
        }

        Optional<Inscripcion> existente = inscripcionRepository.buscarPorUsuarioYEvento(usuarioId, eventoId);
        
        if (existente.isPresent() && existente.get().getEstado() != EstadoInscripcion.CANCELADO) {
            throw new InscripcionDuplicadaException("El usuario ya se encuentra inscrito en este evento.");
        }

        evento.aumentarAforo();
        eventoRepository.guardar(evento);

        Inscripcion nuevaInscripcion = new Inscripcion();
        nuevaInscripcion.setUsuarioId(usuarioId);
        nuevaInscripcion.setEventoId(eventoId);
        nuevaInscripcion.setFechaInscripcion(LocalDateTime.now());
        nuevaInscripcion.setEstado(EstadoInscripcion.INSCRITO);

        return inscripcionRepository.guardar(nuevaInscripcion);
    }
}
