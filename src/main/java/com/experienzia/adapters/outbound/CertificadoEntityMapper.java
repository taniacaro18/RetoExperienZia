package com.experienzia.adapters.outbound;

import com.experienzia.domain.model.Certificado;
import com.experienzia.infrastructure.persistence.entity.CertificadoEntity;
import org.springframework.stereotype.Component;

@Component
public class CertificadoEntityMapper {

    public Certificado toDomain(CertificadoEntity entity) {
        if (entity == null) return null;
        return new Certificado(
                entity.getId(),
                entity.getUsuarioId(),
                entity.getEventoId(),
                entity.getFechaGeneracion(),
                entity.getCodigoUnico()
        );
    }

    public CertificadoEntity toEntity(Certificado domain) {
        if (domain == null) return null;
        CertificadoEntity entity = new CertificadoEntity();
        entity.setId(domain.getId());
        entity.setUsuarioId(domain.getUsuarioId());
        entity.setEventoId(domain.getEventoId());
        entity.setFechaGeneracion(domain.getFechaGeneracion());
        entity.setCodigoUnico(domain.getCodigoUnico());
        return entity;
    }
}
