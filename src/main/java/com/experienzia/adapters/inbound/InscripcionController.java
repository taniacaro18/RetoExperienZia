package com.experienzia.adapters.inbound;

import com.experienzia.adapters.inbound.dto.InscripcionRequestDTO;
import com.experienzia.adapters.inbound.dto.InscripcionResponseDTO;
import com.experienzia.adapters.inbound.mapper.InscripcionDTOMapper;
import com.experienzia.application.usecase.CancelarInscripcionUseCase;
import com.experienzia.application.usecase.CrearInscripcionUseCase;
import com.experienzia.application.usecase.ListarInscripcionesPorEventoUseCase;
import com.experienzia.domain.exception.EventoNoDisponibleException;
import com.experienzia.domain.exception.EventoSinCupoException;
import com.experienzia.domain.exception.InscripcionDuplicadaException;
import com.experienzia.domain.model.Inscripcion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inscripciones")
public class InscripcionController {

    private final CrearInscripcionUseCase crearInscripcionUseCase;
    private final CancelarInscripcionUseCase cancelarInscripcionUseCase;
    private final ListarInscripcionesPorEventoUseCase listarInscripcionesPorEventoUseCase;
    private final InscripcionDTOMapper mapper;

    public InscripcionController(CrearInscripcionUseCase crearInscripcionUseCase,
                                 CancelarInscripcionUseCase cancelarInscripcionUseCase,
                                 ListarInscripcionesPorEventoUseCase listarInscripcionesPorEventoUseCase,
                                 InscripcionDTOMapper mapper) {
        this.crearInscripcionUseCase = crearInscripcionUseCase;
        this.cancelarInscripcionUseCase = cancelarInscripcionUseCase;
        this.listarInscripcionesPorEventoUseCase = listarInscripcionesPorEventoUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> crearInscripcion(@RequestBody InscripcionRequestDTO request) {
        try {
            Inscripcion inscripcion = crearInscripcionUseCase.ejecutar(request.getUsuarioId(), request.getEventoId());
            return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(inscripcion));
        } catch (EventoNoDisponibleException | EventoSinCupoException | InscripcionDuplicadaException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al crear la inscripción");
        }
    }

    @PutMapping("/{id}/cancelar")
    @Transactional
    public ResponseEntity<?> cancelarInscripcion(@PathVariable Long id) {
        try {
            Inscripcion inscripcion = cancelarInscripcionUseCase.ejecutar(id);
            return ResponseEntity.ok(mapper.toDto(inscripcion));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al cancelar la inscripción");
        }
    }

    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<List<InscripcionResponseDTO>> listarPorEvento(@PathVariable Long eventoId) {
        List<Inscripcion> inscripciones = listarInscripcionesPorEventoUseCase.ejecutar(eventoId);
        List<InscripcionResponseDTO> response = inscripciones.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
