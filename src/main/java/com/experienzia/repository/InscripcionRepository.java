package com.experienzia.repository;

import com.experienzia.entity.EstadoInscripcion;
import com.experienzia.entity.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    Optional<Inscripcion> findByUsuarioIdAndEventoId(Long usuarioId, Long eventoId);

    Optional<Inscripcion> findByCodigoQR(String codigoQR);

    List<Inscripcion> findByEventoId(Long eventoId);

    List<Inscripcion> findByUsuarioId(Long usuarioId);

    long countByEventoIdAndEstado(Long eventoId, EstadoInscripcion estado);

    long countByEventoIdAndEstadoAndFechaCheckOutIsNull(Long eventoId, EstadoInscripcion estado);

    @Query("SELECT i.eventoId, COUNT(i) AS total FROM Inscripcion i GROUP BY i.eventoId ORDER BY total DESC")
    List<Object[]> findEventosPopulares();

    @Query("SELECT i.usuarioId FROM Inscripcion i WHERE i.eventoId = :eventoId")
    List<Long> findUsuarioIdsByEventoId(@Param("eventoId") Long eventoId);
}
