package com.experienzia.application.usecase;

import com.experienzia.domain.model.Auditoria;
import com.experienzia.domain.port.AuditoriaRepository;
import java.util.List;

public class ListarAuditoriasUseCase {

    private final AuditoriaRepository auditoriaRepository;

    public ListarAuditoriasUseCase(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public List<Auditoria> listarTodo() {
        return auditoriaRepository.listarTodo();
    }

    public List<Auditoria> listarPorUsuario(Long usuarioId) {
        return auditoriaRepository.listarPorUsuario(usuarioId);
    }

    public List<Auditoria> listarPorEntidad(String entidad) {
        return auditoriaRepository.listarPorEntidad(entidad);
    }
}
