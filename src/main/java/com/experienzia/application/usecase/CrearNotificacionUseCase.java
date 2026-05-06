package com.experienzia.application.usecase;

import com.experienzia.domain.model.Notificacion;
import com.experienzia.domain.model.TipoNotificacion;
import com.experienzia.domain.port.NotificacionRepository;
import java.time.LocalDateTime;

public class CrearNotificacionUseCase {

    private final NotificacionRepository notificacionRepository;

    public CrearNotificacionUseCase(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    public Notificacion ejecutar(Long usuarioId, String mensaje, TipoNotificacion tipo) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(usuarioId);
        notificacion.setMensaje(mensaje);
        notificacion.setTipo(tipo);
        notificacion.setLeida(false);
        notificacion.setFecha(LocalDateTime.now());
        
        return notificacionRepository.guardar(notificacion);
    }
}
