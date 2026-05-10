package com.experienzia.service;

import com.experienzia.dto.NotificacionDTO;
import com.experienzia.entity.TipoNotificacion;

import java.util.List;

public interface NotificacionService {
    NotificacionDTO crear(Long usuarioId, String mensaje, TipoNotificacion tipo);
    List<NotificacionDTO> listarPorUsuario(Long usuarioId);
    NotificacionDTO marcarLeida(Long id);
}
