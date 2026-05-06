package com.experienzia.adapters.outbound;

import com.experienzia.domain.model.Inscripcion;
import com.experienzia.infrastructure.persistence.entity.InscripcionEntity;
import org.springframework.stereotype.Component;

@Component
public class InscripcionEntityMapper {

    public Inscripcion toDomain(InscripcionEntity entity) {
        if (entity == null) return null;
        return new Inscripcion(
                entity.getId(),
                entity.getUsuarioId(),
                entity.getEventoId(),
                entity.getFechaInscripcion(),
                entity.getEstado()
        );
    }

    public InscripcionEntity toEntity(Inscripcion domain) {
        if (domain == null) return null;
        InscripcionEntity entity = new InscripcionEntity();
        entity.setId(domain.getId());
        entity.setUsuarioId(domain.getUsuarioId());
        entity.setEventoId(domain.getEventoId());
        entity.setFechaInscripcion(domain.getFechaInscripcion());
        entity.setEstado(domain.getEstado());
        return entity;
    }
}
