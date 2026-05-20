package com.experienzia.service;

import com.experienzia.dto.NotificacionDTO;
import com.experienzia.entity.TipoNotificacion;

import java.util.List;

/**
 * Contrato para notificaciones internas del sistema.
 * Avisamos al usuario cuando pasa algo importante (pago, cancelacion, etc.).
 */
public interface NotificacionService {

    /** Crea una notificacion nueva para un usuario. */
    NotificacionDTO crear(Long usuarioId, String mensaje, TipoNotificacion tipo);

    /** Lista las notificaciones de un usuario ordenadas por fecha. */
    List<NotificacionDTO> listarPorUsuario(Long usuarioId);

    /** Marca una notificacion como leida. */
    NotificacionDTO marcarLeida(Long id);
}
