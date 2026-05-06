package com.experienzia.adapters.inbound;

import com.experienzia.application.usecase.ListarNotificacionesUseCase;
import com.experienzia.application.usecase.MarcarNotificacionLeidaUseCase;
import com.experienzia.domain.model.Notificacion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final ListarNotificacionesUseCase listarNotificacionesUseCase;
    private final MarcarNotificacionLeidaUseCase marcarNotificacionLeidaUseCase;

    public NotificacionController(ListarNotificacionesUseCase listarNotificacionesUseCase,
                                  MarcarNotificacionLeidaUseCase marcarNotificacionLeidaUseCase) {
        this.listarNotificacionesUseCase = listarNotificacionesUseCase;
        this.marcarNotificacionLeidaUseCase = marcarNotificacionLeidaUseCase;
    }

    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<Notificacion>> listarNotificaciones(@PathVariable Long usuarioId) {
        try {
            List<Notificacion> notificaciones = listarNotificacionesUseCase.ejecutar(usuarioId);
            return ResponseEntity.ok(notificaciones);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/leida")
    public ResponseEntity<?> marcarLeida(@PathVariable Long id) {
        try {
            Notificacion notificacion = marcarNotificacionLeidaUseCase.ejecutar(id);
            return ResponseEntity.ok(notificacion);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
