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
 * Controlador REST de notificaciones dentro de la app (avisos para el usuario).
 * URL base: /api/notificaciones
 * Lo usan todos los roles (ADMIN, ORGANIZADOR, ASISTENTE, STAFF) para ver y marcar como leídas.
 */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    // Lista las notificaciones de un usuario (las más nuevas primero). Devuelve lista de NotificacionDTO.
    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<NotificacionDTO>> listar(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(notificacionService.listarPorUsuario(usuarioId));
    }

    // Marca una notificación como leída. Devuelve la NotificacionDTO actualizada.
    @PutMapping("/{id}/leida")
    public ResponseEntity<NotificacionDTO> marcarLeida(@PathVariable Long id) {
        return ResponseEntity.ok(notificacionService.marcarLeida(id));
    }
}
