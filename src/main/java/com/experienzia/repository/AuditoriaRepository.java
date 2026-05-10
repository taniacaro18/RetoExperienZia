package com.experienzia.repository;

import com.experienzia.entity.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    List<Auditoria> findAllByOrderByFechaDesc();
    List<Auditoria> findByUsuarioIdOrderByFechaDesc(Long usuarioId);
    List<Auditoria> findByEntidadOrderByFechaDesc(String entidad);
    List<Auditoria> findByEntidadAndEntidadIdAndAccionInOrderByFechaDesc(
            String entidad, Long entidadId, List<String> acciones);
}
