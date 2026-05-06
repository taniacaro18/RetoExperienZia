package com.experienzia.application.usecase;

import com.experienzia.domain.model.Auditoria;
import com.experienzia.domain.port.AuditoriaRepository;
import java.time.LocalDateTime;

public class CrearAuditoriaUseCase {

    private final AuditoriaRepository auditoriaRepository;

    public CrearAuditoriaUseCase(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public Auditoria ejecutar(Long usuarioId, String accion, String entidad, Long entidadId) {
        if (accion == null || accion.trim().isEmpty()) {
            throw new IllegalArgumentException("La acción de auditoría no puede estar vacía.");
        }
        if (entidad == null || entidad.trim().isEmpty()) {
            throw new IllegalArgumentException("La entidad de auditoría no puede estar vacía.");
        }

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuarioId(usuarioId);
        auditoria.setAccion(accion);
        auditoria.setEntidad(entidad);
        auditoria.setEntidadId(entidadId);
        auditoria.setFecha(LocalDateTime.now());

        return auditoriaRepository.guardar(auditoria);
    }
}
