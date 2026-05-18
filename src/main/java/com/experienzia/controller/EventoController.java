package com.experienzia.controller;

import com.experienzia.dto.CancelarEventoDTO;
import com.experienzia.dto.DisponibilidadSalonDTO;
import com.experienzia.dto.EventoDTO;
import com.experienzia.dto.EventoNovedadDTO;
import com.experienzia.dto.RechazarEventoDTO;
import com.experienzia.entity.TipoNotificacion;
import com.experienzia.exceptions.CustomException;
import com.experienzia.service.AuditoriaService;
import com.experienzia.service.EventoService;
import com.experienzia.service.NotificacionService;
import com.experienzia.spec.EventoSpecification.EventoSearchCriteria;
import com.experienzia.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<EventoDTO> editar(@PathVariable Long id, @RequestBody EventoDTO dto,
                                            HttpServletRequest request) {
        EventoDTO actualizado = eventoService.editar(id, dto);
        auditoriaService.registrar(dto.getOrganizadorId(), "EVENTO_EDITADO", "Evento", actualizado.getId(),
                ClientIpResolver.resolve(request));
        String detalle;
        if (actualizado.getEstado() == com.experienzia.entity.EstadoEvento.PENDIENTE_SUPLEMENTO) {
            detalle = "Tu evento \"" + actualizado.getNombre()
                    + "\" requiere pago adicional por horas ampliadas. Sube el comprobante solo por la diferencia.";
        } else if (actualizado.getEstado() == com.experienzia.entity.EstadoEvento.PENDIENTE_REVISION) {
            detalle = "Tu evento \"" + actualizado.getNombre()
                    + "\" quedó pendiente de aprobación administrativa por los cambios realizados.";
        } else if (actualizado.getEstado() == com.experienzia.entity.EstadoEvento.PENDIENTE_CANCELACION) {
            detalle = "Solicitaste cancelar el evento \"" + actualizado.getNombre()
                    + "\". Un administrador revisará tu solicitud.";
        } else if (actualizado.getEstado() == com.experienzia.entity.EstadoEvento.PENDIENTE) {
            detalle = "Tu evento \"" + actualizado.getNombre()
                    + "\" fue actualizado y quedó PENDIENTE de re-aprobación por el administrador.";
        } else {
            detalle = "Tu evento \"" + actualizado.getNombre() + "\" fue actualizado.";
            if (actualizado.getCosto() != null && actualizado.getCosto() > 0) {
                detalle += " Si ampliaste la duración y ya tenías un pago aprobado, revisa la sección Pagos: puede pedirse un comprobante solo por la diferencia.";
            }
        }
        notificacionService.crear(actualizado.getOrganizadorId(), detalle, TipoNotificacion.INFO);
        return ResponseEntity.ok(actualizado);
    }

    @PostMapping("/{id:\\d+}/aprobar")
    public ResponseEntity<EventoDTO> aprobar(@PathVariable Long id,
                                             @RequestParam(required = false) Long adminId,
                                             HttpServletRequest request) {
        EventoDTO aprobado = eventoService.aprobar(id);
        String detalleAprobacion;
        if (aprobado.getEstado() == com.experienzia.entity.EstadoEvento.PENDIENTE_SUPLEMENTO) {
            detalleAprobacion = "El administrador aprobó la ampliación de horas de \""
                    + aprobado.getNombre()
                    + "\". Sube en Pagos el comprobante solo por el incremento pendiente.";
        } else if (aprobado.getEstado() == com.experienzia.entity.EstadoEvento.APROBADO) {
            detalleAprobacion = "Tu solicitud del evento \"" + aprobado.getNombre()
                    + "\" fue aprobada. Sube el comprobante de pago en Pagos para activarlo.";
        } else {
            detalleAprobacion = "Tu solicitud del evento \"" + aprobado.getNombre()
                    + "\" fue aprobada por el administrador.";
        }
        notificacionService.crear(aprobado.getOrganizadorId(), detalleAprobacion, TipoNotificacion.INFO);
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

    @PostMapping("/{id:\\d+}/cancelar")
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

    @GetMapping("/{id:\\d+}/novedades")
    public ResponseEntity<List<EventoNovedadDTO>> novedades(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.listarNovedades(id));
    }

    @PostMapping("/{id:\\d+}/cancelacion/aprobar")
    public ResponseEntity<EventoDTO> aprobarCancelacion(@PathVariable Long id,
                                                        @RequestParam(required = false) Long adminId,
                                                        HttpServletRequest request) {
        EventoDTO dto = eventoService.aprobarCancelacion(id);
        auditoriaService.registrar(adminId, "EVENTO_CANCELACION_APROBADA", "Evento", dto.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id:\\d+}/cancelacion/rechazar")
    public ResponseEntity<EventoDTO> rechazarCancelacion(@PathVariable Long id,
                                                         @RequestBody(required = false) RechazarEventoDTO body,
                                                         @RequestParam(required = false) Long adminId,
                                                         HttpServletRequest request) {
        String motivo = body != null ? body.getMotivo() : null;
        EventoDTO dto = eventoService.rechazarCancelacion(id, motivo);
        auditoriaService.registrar(adminId, "EVENTO_CANCELACION_RECHAZADA", "Evento", dto.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(dto);
    }

    /** Calendario de ocupación del salón (organizador y admin). Ruta fija antes de /{id}. */
    @GetMapping("/salon/disponibilidad")
    public ResponseEntity<DisponibilidadSalonDTO> disponibilidadSalon(
            @RequestParam(required = false) String ubicacion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) Long excluirEventoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime propuestaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime propuestaFin) {
        return ResponseEntity.ok(eventoService.consultarDisponibilidadSalon(
                ubicacion, desde, hasta, excluirEventoId, propuestaInicio, propuestaFin));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<EventoDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<EventoDTO>> listarTodos() {
        return ResponseEntity.ok(eventoService.listarTodos());
    }

    @GetMapping("/catalogo/publicos")
    public ResponseEntity<List<EventoDTO>> listarCatalogoPublicosActivos() {
        return ResponseEntity.ok(eventoService.listarCatalogoPublicoActivo());
    }

    @GetMapping("/catalogo/publicos/{id:\\d+}")
    public ResponseEntity<EventoDTO> obtenerPublico(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtenerParaCatalogoPublico(id));
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
