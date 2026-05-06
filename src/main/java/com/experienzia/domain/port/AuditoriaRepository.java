package com.experienzia.domain.port;

import com.experienzia.domain.model.Auditoria;
import java.util.List;

public interface AuditoriaRepository {
    Auditoria guardar(Auditoria auditoria);
    List<Auditoria> listarTodo();
    List<Auditoria> listarPorUsuario(Long usuarioId);
    List<Auditoria> listarPorEntidad(String entidad);
}
