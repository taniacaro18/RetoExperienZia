package com.experienzia.infrastructure.persistence.repository;

import com.experienzia.infrastructure.persistence.entity.NotificacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionJpaRepository extends JpaRepository<NotificacionEntity, Long> {
    List<NotificacionEntity> findByUsuarioIdOrderByFechaDesc(Long usuarioId);
}
