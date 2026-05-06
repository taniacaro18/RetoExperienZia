package com.experienzia.adapters.inbound;

import com.experienzia.application.usecase.AprobarPagoUseCase;
import com.experienzia.application.usecase.CrearPagoUseCase;
import com.experienzia.application.usecase.RechazarPagoUseCase;
import com.experienzia.domain.model.Pago;
import com.experienzia.domain.port.PagoRepository;
import com.experienzia.infrastructure.storage.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final CrearPagoUseCase crearPagoUseCase;
    private final AprobarPagoUseCase aprobarPagoUseCase;
    private final RechazarPagoUseCase rechazarPagoUseCase;
    private final PagoRepository pagoRepository;
    private final FileStorageService fileStorageService;

    public PagoController(CrearPagoUseCase crearPagoUseCase,
                          AprobarPagoUseCase aprobarPagoUseCase,
                          RechazarPagoUseCase rechazarPagoUseCase,
                          PagoRepository pagoRepository,
                          FileStorageService fileStorageService) {
        this.crearPagoUseCase = crearPagoUseCase;
        this.aprobarPagoUseCase = aprobarPagoUseCase;
        this.rechazarPagoUseCase = rechazarPagoUseCase;
        this.pagoRepository = pagoRepository;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> crearPago(@RequestParam Long inscripcionId,
                                       @RequestParam MultipartFile archivo) {
        try {
            String comprobanteUrl = fileStorageService.guardarArchivo(archivo);
            Pago pago = crearPagoUseCase.ejecutar(inscripcionId, comprobanteUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(pago);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al guardar el archivo comprobante.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno al procesar el pago.");
        }
    }

    @PutMapping("/{id}/aprobar")
    @Transactional
    public ResponseEntity<?> aprobarPago(@PathVariable Long id) {
        try {
            Pago pago = aprobarPagoUseCase.ejecutar(id);
            return ResponseEntity.ok(pago);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al aprobar el pago.");
        }
    }

    @PutMapping("/{id}/rechazar")
    @Transactional
    public ResponseEntity<?> rechazarPago(@PathVariable Long id) {
        try {
            Pago pago = rechazarPagoUseCase.ejecutar(id);
            return ResponseEntity.ok(pago);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al rechazar el pago.");
        }
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<Pago>> listarPendientes() {
        List<Pago> pendientes = pagoRepository.listarPendientes();
        return ResponseEntity.ok(pendientes);
    }
}
