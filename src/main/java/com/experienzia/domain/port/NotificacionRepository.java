package com.experienzia.domain.port;

import com.experienzia.domain.model.Notificacion;
import java.util.List;
import java.util.Optional;

public interface NotificacionRepository {
    Notificacion guardar(Notificacion notificacion);
    List<Notificacion> listarPorUsuario(Long usuarioId);
    Optional<Notificacion> buscarPorId(Long id);
}
