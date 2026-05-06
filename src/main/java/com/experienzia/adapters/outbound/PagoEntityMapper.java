package com.experienzia.adapters.outbound;

import com.experienzia.domain.model.Pago;
import com.experienzia.infrastructure.persistence.entity.PagoEntity;
import org.springframework.stereotype.Component;

@Component
public class PagoEntityMapper {

    public Pago toDomain(PagoEntity entity) {
        if (entity == null) return null;
        return new Pago(
                entity.getId(),
                entity.getInscripcionId(),
                entity.getComprobanteUrl(),
                entity.getEstado(),
                entity.getFecha()
        );
    }

    public PagoEntity toEntity(Pago domain) {
        if (domain == null) return null;
        PagoEntity entity = new PagoEntity();
        entity.setId(domain.getId());
        entity.setInscripcionId(domain.getInscripcionId());
        entity.setComprobanteUrl(domain.getComprobanteUrl());
        entity.setEstado(domain.getEstado());
        entity.setFecha(domain.getFecha());
        return entity;
    }
}
