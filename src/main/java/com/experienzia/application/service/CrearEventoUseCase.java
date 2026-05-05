package com.experienzia.application.service;

import com.experienzia.domain.model.Evento;
import com.experienzia.domain.port.EventoRepository;

public class CrearEventoUseCase {

    private final EventoRepository eventoRepository;

    public CrearEventoUseCase(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    /**
     * HU-007: Creación de evento
     * 
     * @param evento Los datos del evento a crear
     * @param organizadorId El ID del organizador que crea el evento
     * @return El evento creado y persistido
     */
    public Evento crearEvento(Evento evento, Long organizadorId) {
        // 1. Asociamos el organizador
        evento.setOrganizadorId(organizadorId);
        
        // 2. Delegamos la lógica al dominio:
        // El método crearEvento() de la entidad ya se encarga internamente de:
        // - Validar que el aforoMaximo > 0 (si no, lanza DatosInvalidosException)
        // - Asignar el estado PENDIENTE
        // - Iniciar el aforoActual en 0
        evento.crearEvento();
        
        // 3. Persistimos a través del puerto
        return eventoRepository.guardar(evento);
    }
}
