package com.experienzia.adapters.outbound;

import com.experienzia.domain.model.Notificacion;
import com.experienzia.infrastructure.persistence.entity.NotificacionEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificacionEntityMapper {

    public Notificacion toDomain(NotificacionEntity entity) {
        if (entity == null) return null;
        return new Notificacion(
                entity.getId(),
                entity.getUsuarioId(),
                entity.getMensaje(),
                entity.getTipo(),
                entity.isLeida(),
                entity.getFecha()
        );
    }

    public NotificacionEntity toEntity(Notificacion domain) {
        if (domain == null) return null;
        NotificacionEntity entity = new NotificacionEntity();
        entity.setId(domain.getId());
        entity.setUsuarioId(domain.getUsuarioId());
        entity.setMensaje(domain.getMensaje());
        entity.setTipo(domain.getTipo());
        entity.setLeida(domain.isLeida());
        entity.setFecha(domain.getFecha());
        return entity;
    }
}
