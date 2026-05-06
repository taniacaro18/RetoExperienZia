package com.experienzia.adapters.inbound;

import com.experienzia.application.usecase.ListarAuditoriasUseCase;
import com.experienzia.domain.model.Auditoria;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final ListarAuditoriasUseCase listarAuditoriasUseCase;

    public AuditoriaController(ListarAuditoriasUseCase listarAuditoriasUseCase) {
        this.listarAuditoriasUseCase = listarAuditoriasUseCase;
    }

    @GetMapping
    public ResponseEntity<List<Auditoria>> listarTodo() {
        return ResponseEntity.ok(listarAuditoriasUseCase.listarTodo());
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Auditoria>> listarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(listarAuditoriasUseCase.listarPorUsuario(usuarioId));
    }

    @GetMapping("/entidad/{tipo}")
    public ResponseEntity<List<Auditoria>> listarPorEntidad(@PathVariable String tipo) {
        return ResponseEntity.ok(listarAuditoriasUseCase.listarPorEntidad(tipo));
    }
}
