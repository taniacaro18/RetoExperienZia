package com.experienzia.controller;

import com.experienzia.dto.CancelarEventoDTO;
import com.experienzia.dto.EventoDTO;
import com.experienzia.dto.RechazarEventoDTO;
import com.experienzia.entity.TipoNotificacion;
import com.experienzia.exceptions.CustomException;
import com.experienzia.service.AuditoriaService;
import com.experienzia.service.EventoService;
import com.experienzia.service.NotificacionService;
import com.experienzia.spec.EventoSpecification.EventoSearchCriteria;
import com.experienzia.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;

    public EventoController(EventoService eventoService,
                            NotificacionService notificacionService,
                            AuditoriaService auditoriaService) {
        this.eventoService = eventoService;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping
    public ResponseEntity<EventoDTO> crear(@RequestBody EventoDTO dto, HttpServletRequest request) {
        EventoDTO creado = eventoService.crear(dto);
        auditoriaService.registrar(creado.getOrganizadorId(), "EVENTO_CREADO", "Evento", creado.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoDTO> editar(@PathVariable Long id, @RequestBody EventoDTO dto,
                                            HttpServletRequest request) {
        EventoDTO actualizado = eventoService.editar(id, dto);
        auditoriaService.registrar(dto.getOrganizadorId(), "EVENTO_EDITADO", "Evento", actualizado.getId(),
                ClientIpResolver.resolve(request));
        notificacionService.crear(actualizado.getOrganizadorId(),
                "Tu evento \"" + actualizado.getNombre() + "\" fue actualizado y quedó en estado PENDIENTE de re-aprobación.",
                TipoNotificacion.INFO);
        return ResponseEntity.ok(actualizado);
    }

    @PostMapping("/{id}/aprobar")
    public ResponseEntity<EventoDTO> aprobar(@PathVariable Long id,
                                             @RequestParam(required = false) Long adminId,
                                             HttpServletRequest request) {
        EventoDTO aprobado = eventoService.aprobar(id);
        notificacionService.crear(
                aprobado.getOrganizadorId(),
                "Tu solicitud del evento \"" + aprobado.getNombre()
                        + "\" fue aprobada. Completa el pago cuando aplique para activarlo en el sistema.",
                TipoNotificacion.INFO);
        auditoriaService.registrar(adminId, "EVENTO_APROBADO", "Evento", aprobado.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(aprobado);
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<EventoDTO> rechazar(@PathVariable Long id,
                                              @RequestBody(required = false) RechazarEventoDTO body,
                                              @RequestParam(required = false) Long adminId,
                                              HttpServletRequest request) {
        String motivo = body != null ? body.getMotivo() : null;
        EventoDTO rechazado = eventoService.rechazar(id, motivo);
        notificacionService.crear(
                rechazado.getOrganizadorId(),
                "Tu solicitud del evento \"" + rechazado.getNombre() + "\" fue rechazada."
                        + (motivo != null && !motivo.isBlank() ? " Motivo: " + motivo.trim() : ""),
                TipoNotificacion.ALERTA);
        auditoriaService.registrar(adminId, "EVENTO_RECHAZADO", "Evento", rechazado.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(rechazado);
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<EventoDTO> cancelar(@PathVariable Long id, @RequestBody CancelarEventoDTO body,
                                              HttpServletRequest request) {
        if (body == null || body.getOrganizadorId() == null) {
            throw new CustomException("organizadorId es obligatorio.", HttpStatus.BAD_REQUEST);
        }
        EventoDTO cancelado = eventoService.cancelar(id, body.getOrganizadorId(), body.getMotivo());
        auditoriaService.registrar(body.getOrganizadorId(), "EVENTO_CANCELADO", "Evento", cancelado.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(cancelado);
    }

    @GetMapping
    public ResponseEntity<List<EventoDTO>> listarTodos() {
        return ResponseEntity.ok(eventoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtenerPorId(id));
    }

    @GetMapping("/catalogo/publicos")
    public ResponseEntity<List<EventoDTO>> listarCatalogoPublicosActivos() {
        return ResponseEntity.ok(eventoService.listarCatalogoPublicoActivo());
    }

    @GetMapping("/catalogo/publicos/{id}")
    public ResponseEntity<EventoDTO> obtenerPublico(@PathVariable Long id) {
        EventoDTO dto = eventoService.obtenerPorId(id);
        if (dto.getTipoEvento() != com.experienzia.entity.TipoEvento.PUBLICO
                || dto.getEstado() != com.experienzia.entity.EstadoEvento.ACTIVO) {
            throw new CustomException("Evento no disponible en el catálogo público.", HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/organizador/{organizadorId}")
    public ResponseEntity<List<EventoDTO>> listarMisEventos(@PathVariable Long organizadorId) {
        return ResponseEntity.ok(eventoService.listarPorOrganizador(organizadorId));
    }

    /** HU-011: búsqueda con filtros (nombre, categoría, tipo, estado, fecha). */
    @GetMapping("/buscar")
    public ResponseEntity<List<EventoDTO>> buscar(EventoSearchCriteria criteria) {
        return ResponseEntity.ok(eventoService.buscar(criteria));
    }
}
