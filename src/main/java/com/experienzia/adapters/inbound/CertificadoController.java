package com.experienzia.adapters.inbound;

import com.experienzia.application.usecase.GenerarCertificadoUseCase;
import com.experienzia.application.usecase.ListarCertificadosPorUsuarioUseCase;
import com.experienzia.application.usecase.ValidarCertificadoUseCase;
import com.experienzia.domain.model.Certificado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/certificados")
public class CertificadoController {

    private final GenerarCertificadoUseCase generarCertificadoUseCase;
    private final ListarCertificadosPorUsuarioUseCase listarCertificadosPorUsuarioUseCase;
    private final ValidarCertificadoUseCase validarCertificadoUseCase;

    public CertificadoController(GenerarCertificadoUseCase generarCertificadoUseCase,
                                 ListarCertificadosPorUsuarioUseCase listarCertificadosPorUsuarioUseCase,
                                 ValidarCertificadoUseCase validarCertificadoUseCase) {
        this.generarCertificadoUseCase = generarCertificadoUseCase;
        this.listarCertificadosPorUsuarioUseCase = listarCertificadosPorUsuarioUseCase;
        this.validarCertificadoUseCase = validarCertificadoUseCase;
    }

    @PostMapping("/generar/{inscripcionId}")
    public ResponseEntity<?> generarCertificado(@PathVariable Long inscripcionId) {
        try {
            Certificado certificado = generarCertificadoUseCase.ejecutar(inscripcionId);
            return ResponseEntity.status(HttpStatus.CREATED).body(certificado);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al generar el certificado.");
        }
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Certificado>> listarPorUsuario(@PathVariable Long usuarioId) {
        try {
            List<Certificado> certificados = listarCertificadosPorUsuarioUseCase.ejecutar(usuarioId);
            return ResponseEntity.ok(certificados);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/validar/{codigo}")
    public ResponseEntity<?> validarCertificado(@PathVariable String codigo) {
        try {
            Certificado certificado = validarCertificadoUseCase.ejecutar(codigo);
            return ResponseEntity.ok(certificado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al validar el certificado.");
        }
    }
}
