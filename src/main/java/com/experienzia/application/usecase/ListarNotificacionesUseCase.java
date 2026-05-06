package com.experienzia.application.usecase;

import com.experienzia.domain.model.Notificacion;
import com.experienzia.domain.port.NotificacionRepository;
import java.util.List;

public class ListarNotificacionesUseCase {

    private final NotificacionRepository notificacionRepository;

    public ListarNotificacionesUseCase(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    public List<Notificacion> ejecutar(Long usuarioId) {
        return notificacionRepository.listarPorUsuario(usuarioId);
    }
}
