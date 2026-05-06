package com.experienzia.adapters.outbound;

import com.experienzia.domain.model.Auditoria;
import com.experienzia.infrastructure.persistence.entity.AuditoriaEntity;
import org.springframework.stereotype.Component;

@Component
public class AuditoriaEntityMapper {

    public Auditoria toDomain(AuditoriaEntity entity) {
        if (entity == null) return null;
        return new Auditoria(
                entity.getId(),
                entity.getUsuarioId(),
                entity.getAccion(),
                entity.getEntidad(),
                entity.getEntidadId(),
                entity.getFecha()
        );
    }

    public AuditoriaEntity toEntity(Auditoria domain) {
        if (domain == null) return null;
        AuditoriaEntity entity = new AuditoriaEntity();
        entity.setId(domain.getId());
        entity.setUsuarioId(domain.getUsuarioId());
        entity.setAccion(domain.getAccion());
        entity.setEntidad(domain.getEntidad());
        entity.setEntidadId(domain.getEntidadId());
        entity.setFecha(domain.getFecha());
        return entity;
    }
}
