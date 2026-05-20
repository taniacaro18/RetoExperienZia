package com.experienzia.controller;

import com.experienzia.dto.AuditoriaDTO;
import com.experienzia.service.AuditoriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para consultar el registro de auditoría (quién hizo qué y cuándo).
 * URL base: /api/auditoria
 * Lo usa principalmente el rol ADMIN para revisar acciones del sistema.
 */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    // Lista todos los registros de auditoría del sistema. Devuelve lista de AuditoriaDTO.
    @GetMapping
    public ResponseEntity<List<AuditoriaDTO>> listarTodo() {
        return ResponseEntity.ok(auditoriaService.listarTodo());
    }

    // Lista acciones de auditoría hechas por un usuario. Devuelve lista de AuditoriaDTO.
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<AuditoriaDTO>> listarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(auditoriaService.listarPorUsuario(usuarioId));
    }

    // Lista auditoría filtrada por tipo de entidad (Evento, Usuario, etc.). Devuelve lista de AuditoriaDTO.
    @GetMapping("/entidad/{tipo}")
    public ResponseEntity<List<AuditoriaDTO>> listarPorEntidad(@PathVariable String tipo) {
        return ResponseEntity.ok(auditoriaService.listarPorEntidad(tipo));
    }
}
