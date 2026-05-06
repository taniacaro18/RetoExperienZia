package com.experienzia.application.usecase;

import com.experienzia.domain.model.Notificacion;
import com.experienzia.domain.port.NotificacionRepository;

public class MarcarNotificacionLeidaUseCase {

    private final NotificacionRepository notificacionRepository;

    public MarcarNotificacionLeidaUseCase(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    public Notificacion ejecutar(Long id) {
        Notificacion notificacion = notificacionRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada."));
        
        notificacion.marcarComoLeida();
        
        return notificacionRepository.guardar(notificacion);
    }
}
