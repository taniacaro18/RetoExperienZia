package com.experienzia.repository;

import com.experienzia.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para notificaciones de usuarios.
 */
@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /** Notificaciones de un usuario, mas recientes primero. */
    List<Notificacion> findByUsuarioIdOrderByFechaDesc(Long usuarioId);
}
