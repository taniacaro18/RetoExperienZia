package com.experienzia.infrastructure.persistence.repository;

import com.experienzia.infrastructure.persistence.entity.InscripcionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.experienzia.domain.model.EstadoInscripcion;

import java.util.List;
import java.util.Optional;

@Repository
public interface InscripcionJpaRepository extends JpaRepository<InscripcionEntity, Long> {
    Optional<InscripcionEntity> findByUsuarioIdAndEventoId(Long usuarioId, Long eventoId);
    List<InscripcionEntity> findByEventoId(Long eventoId);

    @Query("SELECT i.eventoId, COUNT(i) as total FROM InscripcionEntity i GROUP BY i.eventoId ORDER BY total DESC")
    List<Object[]> findEventosPopulares();

    @Query("SELECT i.usuarioId FROM InscripcionEntity i WHERE i.eventoId = :eventoId")
    List<Long> findUsuarioIdsByEventoId(@Param("eventoId") Long eventoId);

    long countByEventoIdAndEstado(Long eventoId, EstadoInscripcion estado);
}
