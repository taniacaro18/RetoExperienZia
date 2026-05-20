package com.experienzia.service;

import com.experienzia.dto.AuditoriaDTO;

import java.util.List;

/**
 * Contrato para registrar y consultar la auditoria del sistema.
 * Guardamos quien hizo que y cuando (para trazabilidad).
 */
public interface AuditoriaService {

    /** Atajo para registrar sin IP. */
    default AuditoriaDTO registrar(Long usuarioId, String accion, String entidad, Long entidadId) {
        return registrar(usuarioId, accion, entidad, entidadId, null);
    }

    /** Guarda un registro de auditoria en la base de datos. */
    AuditoriaDTO registrar(Long usuarioId, String accion, String entidad, Long entidadId, String direccionIp);

    /** Todos los registros ordenados por fecha. */
    List<AuditoriaDTO> listarTodo();

    /** Registros de un usuario especifico. */
    List<AuditoriaDTO> listarPorUsuario(Long usuarioId);

    /** Registros filtrados por nombre de entidad (Evento, Pago, etc.). */
    List<AuditoriaDTO> listarPorEntidad(String entidad);
}
