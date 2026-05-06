package com.experienzia.adapters.inbound.mapper;

import com.experienzia.adapters.inbound.dto.InscripcionResponseDTO;
import com.experienzia.domain.model.Inscripcion;
import org.springframework.stereotype.Component;

@Component
public class InscripcionDTOMapper {

    public InscripcionResponseDTO toDto(Inscripcion domain) {
        if (domain == null) return null;
        InscripcionResponseDTO dto = new InscripcionResponseDTO();
        dto.setId(domain.getId());
        dto.setUsuarioId(domain.getUsuarioId());
        dto.setEventoId(domain.getEventoId());
        dto.setFechaInscripcion(domain.getFechaInscripcion());
        dto.setEstado(domain.getEstado() != null ? domain.getEstado().name() : null);
        return dto;
    }
}
