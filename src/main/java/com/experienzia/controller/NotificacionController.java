package com.experienzia.controller;

import com.experienzia.dto.NotificacionDTO;
import com.experienzia.service.NotificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Notificaciones in-app para cualquier rol (ADMIN, ORGANIZADOR, ASISTENTE, STAFF).
 * El propio sistema crea avisos internamente cuando:
 *  - se cancela un evento (a sus asistentes)
 *  - se aprueba/rechaza un pago (al organizador)
 *  - se registra check-in/check-out (al asistente)
 *  - se crea una inscripción masiva (a cada nuevo asistente)
 *  - hay actividad de aprobación/rechazo de un organizador (al organizador)
 */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    /** Lista todas las notificaciones de un usuario (más recientes primero). */
    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<NotificacionDTO>> listar(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(notificacionService.listarPorUsuario(usuarioId));
    }

    /** Marca una notificación como leída. */
    @PutMapping("/{id}/leida")
    public ResponseEntity<NotificacionDTO> marcarLeida(@PathVariable Long id) {
        return ResponseEntity.ok(notificacionService.marcarLeida(id));
    }
}
