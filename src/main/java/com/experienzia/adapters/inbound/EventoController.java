package com.experienzia.adapters.inbound;

import com.experienzia.adapters.dto.EventoDtoMapper;
import com.experienzia.adapters.dto.EventoRequestDTO;
import com.experienzia.adapters.dto.EventoResponseDTO;
import com.experienzia.application.service.*;
import com.experienzia.domain.exception.DatosInvalidosException;
import com.experienzia.domain.exception.EventoNoEncontradoException;
import com.experienzia.domain.exception.UsuarioNoAutorizadoException;
import com.experienzia.domain.model.Evento;
import com.experienzia.domain.port.EventoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final CrearEventoUseCase crearEventoUseCase;
    private final EditarEventoUseCase editarEventoUseCase;
    private final AprobarEventoUseCase aprobarEventoUseCase;
    private final CancelarEventoUseCase cancelarEventoUseCase;
    private final ListarEventosUseCase listarEventosUseCase;

    public EventoController(EventoRepository eventoRepository) {
        this.crearEventoUseCase = new CrearEventoUseCase(eventoRepository);
        this.editarEventoUseCase = new EditarEventoUseCase(eventoRepository);
        this.aprobarEventoUseCase = new AprobarEventoUseCase(eventoRepository);
        this.cancelarEventoUseCase = new CancelarEventoUseCase(eventoRepository);
        this.listarEventosUseCase = new ListarEventosUseCase(eventoRepository);
    }

    @PostMapping
    public ResponseEntity<?> crearEvento(@RequestBody EventoRequestDTO requestDTO) {
        try {
            Evento evento = EventoDtoMapper.toDomain(requestDTO);
            Evento creado = crearEventoUseCase.crearEvento(evento, requestDTO.getOrganizadorId());
            return ResponseEntity.ok(EventoDtoMapper.toDto(creado));
        } catch (DatosInvalidosException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editarEvento(@PathVariable Long id, @RequestBody EventoRequestDTO requestDTO) {
        try {
            Evento actualizado = editarEventoUseCase.editarEvento(
                    id, 
                    requestDTO.getOrganizadorId(), 
                    requestDTO.getNombre(), 
                    requestDTO.getDescripcion(), 
                    requestDTO.getFecha(), 
                    requestDTO.getUbicacion(), 
                    requestDTO.getAforoMaximo(), 
                    requestDTO.getCosto(), 
                    requestDTO.getImagen()
            );
            return ResponseEntity.ok(EventoDtoMapper.toDto(actualizado));
        } catch (EventoNoEncontradoException | DatosInvalidosException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (UsuarioNoAutorizadoException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobarEvento(@PathVariable Long id) {
        try {
            Evento aprobado = aprobarEventoUseCase.aprobarEvento(id);
            return ResponseEntity.ok(EventoDtoMapper.toDto(aprobado));
        } catch (EventoNoEncontradoException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelarEvento(@PathVariable Long id, @RequestParam Long organizadorId) {
        try {
            // El organizadorId viene por Query Param, aunque en un entorno real con JWT se sacaría del token.
            Evento cancelado = cancelarEventoUseCase.cancelarEvento(id, organizadorId);
            return ResponseEntity.ok(EventoDtoMapper.toDto(cancelado));
        } catch (EventoNoEncontradoException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (UsuarioNoAutorizadoException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<EventoResponseDTO>> listarEventos() {
        List<EventoResponseDTO> eventos = listarEventosUseCase.listarTodos().stream()
                .map(EventoDtoMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(eventos);
    }
}
