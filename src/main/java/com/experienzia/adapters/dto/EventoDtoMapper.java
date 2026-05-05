package com.experienzia.adapters.dto;

import com.experienzia.domain.model.Evento;
import com.experienzia.domain.model.TipoEvento;

public class EventoDtoMapper {

    public static Evento toDomain(EventoRequestDTO dto) {
        if (dto == null) return null;
        Evento evento = new Evento();
        evento.setNombre(dto.getNombre());
        evento.setDescripcion(dto.getDescripcion());
        evento.setFecha(dto.getFecha());
        evento.setUbicacion(dto.getUbicacion());
        
        if (dto.getTipoEvento() != null) {
            evento.setTipoEvento(TipoEvento.valueOf(dto.getTipoEvento().toUpperCase()));
        }
        
        evento.setAforoMaximo(dto.getAforoMaximo());
        evento.setCosto(dto.getCosto());
        evento.setImagen(dto.getImagen());
        return evento;
    }

    public static EventoResponseDTO toDto(Evento evento) {
        if (evento == null) return null;
        EventoResponseDTO dto = new EventoResponseDTO();
        dto.setId(evento.getId());
        dto.setNombre(evento.getNombre());
        dto.setDescripcion(evento.getDescripcion());
        dto.setFecha(evento.getFecha());
        dto.setUbicacion(evento.getUbicacion());
        
        if (evento.getTipoEvento() != null) {
            dto.setTipoEvento(evento.getTipoEvento().name());
        }
        
        if (evento.getEstado() != null) {
            dto.setEstado(evento.getEstado().name());
        }
        
        dto.setAforoMaximo(evento.getAforoMaximo());
        dto.setAforoActual(evento.getAforoActual());
        dto.setCosto(evento.getCosto());
        dto.setOrganizadorId(evento.getOrganizadorId());
        dto.setImagen(evento.getImagen());
        
        return dto;
    }
}
