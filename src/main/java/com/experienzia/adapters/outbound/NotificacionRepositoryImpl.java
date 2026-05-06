package com.experienzia.adapters.outbound;

import com.experienzia.domain.model.Notificacion;
import com.experienzia.domain.port.NotificacionRepository;
import com.experienzia.infrastructure.persistence.entity.NotificacionEntity;
import com.experienzia.infrastructure.persistence.repository.NotificacionJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class NotificacionRepositoryImpl implements NotificacionRepository {

    private final NotificacionJpaRepository jpaRepository;
    private final NotificacionEntityMapper mapper;

    public NotificacionRepositoryImpl(NotificacionJpaRepository jpaRepository, NotificacionEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Notificacion guardar(Notificacion notificacion) {
        NotificacionEntity entity = mapper.toEntity(notificacion);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<Notificacion> listarPorUsuario(Long usuarioId) {
        return jpaRepository.findByUsuarioIdOrderByFechaDesc(usuarioId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Notificacion> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }
}
