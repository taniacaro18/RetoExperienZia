package com.experienzia.infrastructure.persistence.repository;

import com.experienzia.domain.model.EstadoPago;
import com.experienzia.infrastructure.persistence.entity.PagoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoJpaRepository extends JpaRepository<PagoEntity, Long> {
    Optional<PagoEntity> findByInscripcionId(Long inscripcionId);
    List<PagoEntity> findByEstado(EstadoPago estado);
}
