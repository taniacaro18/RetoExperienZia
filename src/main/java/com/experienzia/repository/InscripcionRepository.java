package com.experienzia.repository;

import com.experienzia.entity.EstadoInscripcion;
import com.experienzia.entity.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para inscripciones a eventos.
 */
@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    /** Busca si el usuario ya esta inscrito en el evento. */
    Optional<Inscripcion> findByUsuarioIdAndEventoId(Long usuarioId, Long eventoId);

    /** Busca inscripcion por codigo QR (check-in). */
    Optional<Inscripcion> findByCodigoQR(String codigoQR);

    /** Todas las inscripciones de un evento. */
    List<Inscripcion> findByEventoId(Long eventoId);

    /** Inscripciones de un usuario. */
    List<Inscripcion> findByUsuarioId(Long usuarioId);

    /** Cuenta inscripciones en cierto estado (confirmadas, asistio, etc.). */
    long countByEventoIdAndEstado(Long eventoId, EstadoInscripcion estado);

    /** Cuenta quienes estan dentro (check-in sin check-out). */
    long countByEventoIdAndEstadoAndFechaCheckOutIsNull(Long eventoId, EstadoInscripcion estado);

    /** Ranking de eventos por cantidad de inscripciones. */
    @Query("SELECT i.eventoId, COUNT(i) AS total FROM Inscripcion i GROUP BY i.eventoId ORDER BY total DESC")
    List<Object[]> findEventosPopulares();

    /** Solo los ids de usuarios inscritos (para reportes). */
    @Query("SELECT i.usuarioId FROM Inscripcion i WHERE i.eventoId = :eventoId")
    List<Long> findUsuarioIdsByEventoId(@Param("eventoId") Long eventoId);
}
