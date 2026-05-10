package com.experienzia.service;

import com.experienzia.dto.AuditoriaDTO;

import java.util.List;

public interface AuditoriaService {
    AuditoriaDTO registrar(Long usuarioId, String accion, String entidad, Long entidadId);
    List<AuditoriaDTO> listarTodo();
    List<AuditoriaDTO> listarPorUsuario(Long usuarioId);
    List<AuditoriaDTO> listarPorEntidad(String entidad);
}
