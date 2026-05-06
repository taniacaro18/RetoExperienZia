package com.experienzia.infrastructure.persistence.repository;

import com.experienzia.infrastructure.persistence.entity.AuditoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaJpaRepository extends JpaRepository<AuditoriaEntity, Long> {
    List<AuditoriaEntity> findByUsuarioIdOrderByFechaDesc(Long usuarioId);
    List<AuditoriaEntity> findByEntidadOrderByFechaDesc(String entidad);
    List<AuditoriaEntity> findAllByOrderByFechaDesc();
}
