package com.experienzia.service;

import com.experienzia.dto.AuditoriaDTO;

import java.util.List;

public interface AuditoriaService {

    default AuditoriaDTO registrar(Long usuarioId, String accion, String entidad, Long entidadId) {
        return registrar(usuarioId, accion, entidad, entidadId, null);
    }

    AuditoriaDTO registrar(Long usuarioId, String accion, String entidad, Long entidadId, String direccionIp);
    List<AuditoriaDTO> listarTodo();
    List<AuditoriaDTO> listarPorUsuario(Long usuarioId);
    List<AuditoriaDTO> listarPorEntidad(String entidad);
}
