package com.experienzia.repository;

import com.experienzia.entity.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para registros de auditoria.
 */
@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    /** Todos los registros, mas recientes primero. */
    List<Auditoria> findAllByOrderByFechaDesc();

    /** Auditoria de un usuario. */
    List<Auditoria> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    /** Auditoria filtrada por entidad (Evento, Pago, Usuario, etc.). */
    List<Auditoria> findByEntidadOrderByFechaDesc(String entidad);

    /** Busqueda mas especifica por entidad, id y lista de acciones. */
    List<Auditoria> findByEntidadAndEntidadIdAndAccionInOrderByFechaDesc(
            String entidad, Long entidadId, List<String> acciones);
}
