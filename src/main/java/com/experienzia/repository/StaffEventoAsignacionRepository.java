package com.experienzia.repository;

import com.experienzia.entity.StaffEventoAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para asignaciones de staff a eventos.
 */
@Repository
public interface StaffEventoAsignacionRepository extends JpaRepository<StaffEventoAsignacion, Long> {

    /** Verifica si el staff ya esta asignado al evento. */
    boolean existsByStaffUsuarioIdAndEventoId(Long staffUsuarioId, Long eventoId);

    /** Busca la asignacion de un staff en un evento. */
    Optional<StaffEventoAsignacion> findByStaffUsuarioIdAndEventoId(Long staffUsuarioId, Long eventoId);

    /** Todo el staff de un evento. */
    List<StaffEventoAsignacion> findByEventoId(Long eventoId);

    /** Eventos donde trabaja un staff. */
    List<StaffEventoAsignacion> findByStaffUsuarioId(Long staffUsuarioId);
}
