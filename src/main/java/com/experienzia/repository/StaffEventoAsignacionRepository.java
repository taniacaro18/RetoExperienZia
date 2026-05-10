package com.experienzia.repository;

import com.experienzia.entity.StaffEventoAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffEventoAsignacionRepository extends JpaRepository<StaffEventoAsignacion, Long> {
    boolean existsByStaffUsuarioIdAndEventoId(Long staffUsuarioId, Long eventoId);
    Optional<StaffEventoAsignacion> findByStaffUsuarioIdAndEventoId(Long staffUsuarioId, Long eventoId);
    List<StaffEventoAsignacion> findByEventoId(Long eventoId);
    List<StaffEventoAsignacion> findByStaffUsuarioId(Long staffUsuarioId);
}
