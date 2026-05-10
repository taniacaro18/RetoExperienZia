package com.experienzia.controller;

import com.experienzia.dto.CertificadoDTO;
import com.experienzia.service.CertificadoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/certificados")
public class CertificadoController {

    private final CertificadoService certificadoService;

    public CertificadoController(CertificadoService certificadoService) {
        this.certificadoService = certificadoService;
    }

    @PostMapping("/generar/{inscripcionId}")
    public ResponseEntity<CertificadoDTO> generar(@PathVariable Long inscripcionId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(certificadoService.generar(inscripcionId));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<CertificadoDTO>> listarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(certificadoService.listarPorUsuario(usuarioId));
    }

    @GetMapping("/validar/{codigo}")
    public ResponseEntity<CertificadoDTO> validar(@PathVariable String codigo) {
        return ResponseEntity.ok(certificadoService.validarPorCodigo(codigo));
    }

    /** HU-024: generación masiva de certificados para todos los asistentes confirmados de un evento. */
    @PostMapping("/evento/{eventoId}/generar-masivo")
    public ResponseEntity<List<CertificadoDTO>> generarMasivo(@PathVariable Long eventoId,
                                                              @RequestParam(required = false) Long organizadorId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(certificadoService.generarMasivoPorEvento(eventoId, organizadorId));
    }

    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<List<CertificadoDTO>> listarPorEvento(@PathVariable Long eventoId) {
        return ResponseEntity.ok(certificadoService.listarPorEvento(eventoId));
    }
}
