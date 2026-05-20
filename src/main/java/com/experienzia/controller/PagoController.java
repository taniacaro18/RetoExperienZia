package com.experienzia.controller;

import com.experienzia.dto.PagoDTO;
import com.experienzia.dto.RechazarPagoDTO;
import com.experienzia.service.PagoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.experienzia.util.ClientIpResolver;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Optional;

/**
 * Controlador REST de pagos de tarifas de eventos (comprobantes).
 * URL base: /api/pagos
 * Lo usa el ORGANIZADOR (subir comprobante) y el ADMIN (aprobar o rechazar pagos).
 */
@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    // Registra el pago de un evento subiendo el comprobante. Devuelve PagoDTO con código 201.
    @PostMapping
    public ResponseEntity<PagoDTO> registrar(@RequestParam Long eventoId,
                                             @RequestParam Long organizadorId,
                                             @RequestParam MultipartFile archivo,
                                             HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pagoService.registrar(eventoId, organizadorId, archivo, ClientIpResolver.resolve(request)));
    }

    // Aprueba un pago pendiente (acción del admin). Devuelve el PagoDTO actualizado.
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<PagoDTO> aprobar(@PathVariable Long id,
                                           @RequestParam(required = false) Long aprobadorId,
                                           HttpServletRequest request) {
        return ResponseEntity.ok(pagoService.aprobar(id, aprobadorId, ClientIpResolver.resolve(request)));
    }

    // Rechaza un pago con un motivo. Devuelve el PagoDTO actualizado.
    @PutMapping("/{id}/rechazar")
    public ResponseEntity<PagoDTO> rechazar(@PathVariable Long id,
                                           @RequestBody RechazarPagoDTO body,
                                           HttpServletRequest request) {
        return ResponseEntity.ok(pagoService.rechazar(id,
                body == null ? null : body.getMotivo(),
                body == null ? null : body.getAprobadorId(),
                ClientIpResolver.resolve(request)));
    }

    // Lista pagos que aún no fueron aprobados ni rechazados. Devuelve lista de PagoDTO.
    @GetMapping("/pendientes")
    public ResponseEntity<List<PagoDTO>> listarPendientes() {
        return ResponseEntity.ok(pagoService.listarPendientes());
    }

    // Historial completo de todos los pagos. Devuelve lista de PagoDTO.
    @GetMapping
    public ResponseEntity<List<PagoDTO>> listarTodos() {
        return ResponseEntity.ok(pagoService.listarTodos());
    }

    // Lista los pagos de un organizador. Devuelve lista de PagoDTO.
    @GetMapping("/organizador/{organizadorId}")
    public ResponseEntity<List<PagoDTO>> listarPorOrganizador(@PathVariable Long organizadorId) {
        return ResponseEntity.ok(pagoService.listarPorOrganizador(organizadorId));
    }

    // Obtiene el pago de un evento si existe. Devuelve PagoDTO o 404 si no hay pago.
    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<PagoDTO> obtenerPorEvento(@PathVariable Long eventoId) {
        Optional<PagoDTO> dto = pagoService.obtenerPorEvento(eventoId);
        return dto.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
