package com.experienzia.controller;

import com.experienzia.dto.AforoEnVivoDTO;
import com.experienzia.dto.AsignarStaffDTO;
import com.experienzia.dto.AsistenteEventoDTO;
import com.experienzia.dto.CargaAsistentesManualDTO;
import com.experienzia.dto.CheckInDTO;
import com.experienzia.dto.EventoStaffDTO;
import com.experienzia.dto.InscripcionDTO;
import com.experienzia.dto.ResultadoCargaAsistentesDTO;
import com.experienzia.dto.StaffAsignadoDTO;
import com.experienzia.entity.FuncionStaff;
import com.experienzia.exceptions.CustomException;
import com.experienzia.service.AuditoriaService;
import com.experienzia.service.InscripcionService;
import com.experienzia.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controlador REST para inscripciones a eventos, check-in/check-out y gestión de staff.
 * No tiene una sola URL base: usa rutas como /api/inscripciones y /api/eventos/...
 * Lo usan asistentes (inscribirse), organizadores (cargar asistentes, asignar staff)
 * y staff (marcar entrada/salida y ver listas).
 */
@RestController
public class InscripcionController {

    private final InscripcionService inscripcionService;
    private final AuditoriaService auditoriaService;

    public InscripcionController(InscripcionService inscripcionService, AuditoriaService auditoriaService) {
        this.inscripcionService = inscripcionService;
        this.auditoriaService = auditoriaService;
    }

    // Crea una inscripción de un usuario a un evento. Devuelve la inscripción creada con código 201.
    @PostMapping("/api/inscripciones")
    public ResponseEntity<InscripcionDTO> crear(@RequestBody InscripcionDTO dto, HttpServletRequest request) {
        InscripcionDTO ins = inscripcionService.inscribir(dto.getUsuarioId(), dto.getEventoId());
        auditoriaService.registrar(ins.getUsuarioId(), "INSCRIPCION_CREADA", "Inscripcion", ins.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ins);
    }

    // Cancela una inscripción por su id. Devuelve la inscripción actualizada.
    @PutMapping("/api/inscripciones/{id}/cancelar")
    public ResponseEntity<InscripcionDTO> cancelar(@PathVariable Long id, HttpServletRequest request) {
        InscripcionDTO ins = inscripcionService.cancelar(id);
        auditoriaService.registrar(ins.getUsuarioId(), "INSCRIPCION_CANCELADA", "Inscripcion", ins.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(ins);
    }

    // Lista todas las inscripciones de un evento. Devuelve una lista de InscripcionDTO.
    @GetMapping("/api/inscripciones/evento/{eventoId}")
    public ResponseEntity<List<InscripcionDTO>> listarPorEvento(@PathVariable Long eventoId) {
        return ResponseEntity.ok(inscripcionService.listarPorEvento(eventoId));
    }

    // Lista las inscripciones de un usuario. Devuelve una lista de InscripcionDTO.
    @GetMapping("/api/inscripciones/usuario/{usuarioId}")
    public ResponseEntity<List<InscripcionDTO>> listarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(inscripcionService.listarPorUsuario(usuarioId));
    }

    // Marca check-in manual (el staff indica quién entró). Devuelve la inscripción actualizada.
    @PutMapping("/api/inscripciones/{id}/check-in")
    public ResponseEntity<InscripcionDTO> checkIn(@PathVariable Long id, @RequestBody CheckInDTO body,
                                                    HttpServletRequest request) {
        if (body == null || body.getStaffUsuarioId() == null) {
            throw new CustomException("staffUsuarioId es obligatorio.", HttpStatus.BAD_REQUEST);
        }
        InscripcionDTO ins = inscripcionService.checkIn(id, body.getStaffUsuarioId());
        auditoriaService.registrar(body.getStaffUsuarioId(), "CHECK_IN", "Inscripcion", ins.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(ins);
    }

    // Marca check-out manual (salida del asistente). Devuelve la inscripción actualizada.
    @PutMapping("/api/inscripciones/{id}/check-out")
    public ResponseEntity<InscripcionDTO> checkOut(@PathVariable Long id, @RequestBody CheckInDTO body,
                                                     HttpServletRequest request) {
        if (body == null || body.getStaffUsuarioId() == null) {
            throw new CustomException("staffUsuarioId es obligatorio.", HttpStatus.BAD_REQUEST);
        }
        InscripcionDTO ins = inscripcionService.checkOut(id, body.getStaffUsuarioId());
        auditoriaService.registrar(body.getStaffUsuarioId(), "CHECK_OUT", "Inscripcion", ins.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(ins);
    }

    // Check-in leyendo el código QR del asistente. Devuelve la inscripción actualizada.
    @PostMapping("/api/inscripciones/check-in/qr")
    public ResponseEntity<InscripcionDTO> checkInPorQR(@RequestBody CheckInDTO body, HttpServletRequest request) {
        if (body == null || body.getStaffUsuarioId() == null || body.getCodigoQR() == null) {
            throw new CustomException("staffUsuarioId y codigoQR son obligatorios.", HttpStatus.BAD_REQUEST);
        }
        InscripcionDTO ins = inscripcionService.checkInPorQR(body.getCodigoQR(), body.getStaffUsuarioId(), body.getEventoId());
        auditoriaService.registrar(body.getStaffUsuarioId(), "CHECK_IN_QR", "Inscripcion", ins.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(ins);
    }

    // Check-out leyendo el código QR del asistente. Devuelve la inscripción actualizada.
    @PostMapping("/api/inscripciones/check-out/qr")
    public ResponseEntity<InscripcionDTO> checkOutPorQR(@RequestBody CheckInDTO body, HttpServletRequest request) {
        if (body == null || body.getStaffUsuarioId() == null || body.getCodigoQR() == null) {
            throw new CustomException("staffUsuarioId y codigoQR son obligatorios.", HttpStatus.BAD_REQUEST);
        }
        InscripcionDTO ins = inscripcionService.checkOutPorQR(body.getCodigoQR(), body.getStaffUsuarioId(), body.getEventoId());
        auditoriaService.registrar(body.getStaffUsuarioId(), "CHECK_OUT_QR", "Inscripcion", ins.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(ins);
    }

    // Carga asistentes a mano (lista en JSON). Devuelve resumen de cuántos se crearon o fallaron.
    @PostMapping("/api/eventos/{eventoId}/asistentes/carga-manual")
    public ResponseEntity<ResultadoCargaAsistentesDTO> cargaManual(@PathVariable Long eventoId,
                                                                   @RequestBody CargaAsistentesManualDTO body,
                                                                   HttpServletRequest request) {
        if (body == null || body.getOrganizadorId() == null || body.getFilas() == null) {
            throw new CustomException("organizadorId y filas son obligatorios.", HttpStatus.BAD_REQUEST);
        }
        ResultadoCargaAsistentesDTO r = inscripcionService.cargarAsistentesManual(eventoId, body.getOrganizadorId(), body.getFilas());
        auditoriaService.registrar(body.getOrganizadorId(), "ASISTENTES_CARGA_MANUAL", "Evento", eventoId,
                ClientIpResolver.resolve(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(r);
    }

    // Carga asistentes desde un archivo CSV. Devuelve el mismo resumen que la carga manual.
    @PostMapping(path = "/api/eventos/{eventoId}/asistentes/carga-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResultadoCargaAsistentesDTO> cargaCsv(@PathVariable Long eventoId,
                                                                @RequestParam Long organizadorId,
                                                                @RequestPart("archivo") MultipartFile archivo,
                                                                HttpServletRequest request) {
        if (archivo == null || archivo.isEmpty()) {
            throw new CustomException("Adjunte el archivo CSV en el campo archivo.", HttpStatus.BAD_REQUEST);
        }
        try {
            String contenido = new String(archivo.getBytes(), StandardCharsets.UTF_8);
            ResultadoCargaAsistentesDTO r = inscripcionService.cargarAsistentesCsv(eventoId, organizadorId, contenido);
            auditoriaService.registrar(organizadorId, "ASISTENTES_CARGA_CSV", "Evento", eventoId,
                    ClientIpResolver.resolve(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(r);
        } catch (java.io.IOException e) {
            throw new CustomException("No se pudo leer el archivo CSV.", HttpStatus.BAD_REQUEST);
        }
    }

    // Asigna un usuario staff a un evento con una función. Devuelve vacío con código 201.
    @PostMapping("/api/eventos/{eventoId}/staff/asignacion")
    public ResponseEntity<Void> asignarStaff(@PathVariable Long eventoId, @RequestBody AsignarStaffDTO body,
                                             HttpServletRequest request) {
        if (body == null || body.getOrganizadorId() == null || body.getStaffUsuarioId() == null) {
            throw new CustomException("organizadorId y staffUsuarioId son obligatorios.", HttpStatus.BAD_REQUEST);
        }
        FuncionStaff funcion = parseFuncion(body.getFuncion());
        inscripcionService.asignarStaff(eventoId, body.getOrganizadorId(), body.getStaffUsuarioId(), funcion);
        auditoriaService.registrar(body.getOrganizadorId(), "STAFF_ASIGNADO_" + funcion, "Evento", eventoId,
                ClientIpResolver.resolve(request));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // Cambia la función del staff en el evento. Devuelve los datos del staff actualizado.
    @PutMapping("/api/eventos/{eventoId}/staff/{staffUsuarioId}/funcion")
    public ResponseEntity<StaffAsignadoDTO> cambiarFuncionStaff(@PathVariable Long eventoId,
                                                                @PathVariable Long staffUsuarioId,
                                                                @RequestParam Long organizadorId,
                                                                @RequestBody AsignarStaffDTO body,
                                                                HttpServletRequest request) {
        FuncionStaff funcion = parseFuncion(body == null ? null : body.getFuncion());
        StaffAsignadoDTO dto = inscripcionService.cambiarFuncionStaff(eventoId, organizadorId, staffUsuarioId, funcion);
        auditoriaService.registrar(organizadorId, "STAFF_FUNCION_CAMBIADA_" + funcion, "Evento", eventoId,
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(dto);
    }

    // Quita al staff del evento. Devuelve vacío con código 204 (sin contenido).
    @DeleteMapping("/api/eventos/{eventoId}/staff/{staffUsuarioId}")
    public ResponseEntity<Void> desasignarStaff(@PathVariable Long eventoId,
                                                @PathVariable Long staffUsuarioId,
                                                @RequestParam Long organizadorId,
                                                HttpServletRequest request) {
        inscripcionService.desasignarStaff(eventoId, organizadorId, staffUsuarioId);
        auditoriaService.registrar(organizadorId, "STAFF_DESASIGNADO", "Evento", eventoId,
                ClientIpResolver.resolve(request));
        return ResponseEntity.noContent().build();
    }

    // Lista solo los ids de staff del evento (versión simple). Devuelve lista de números Long.
    @GetMapping("/api/eventos/{eventoId}/staff/ids")
    public ResponseEntity<List<Long>> listarStaffIds(@PathVariable Long eventoId) {
        return ResponseEntity.ok(inscripcionService.listarStaffIdsPorEvento(eventoId));
    }

    // Lista el staff del evento con nombre y función. Devuelve lista de StaffAsignadoDTO.
    @GetMapping("/api/eventos/{eventoId}/staff")
    public ResponseEntity<List<StaffAsignadoDTO>> listarStaffAsignados(@PathVariable Long eventoId) {
        return ResponseEntity.ok(inscripcionService.listarStaffPorEvento(eventoId));
    }

    // Lista los eventos donde trabaja un staff. Devuelve lista de EventoStaffDTO.
    @GetMapping("/api/staff/{staffUsuarioId}/eventos")
    public ResponseEntity<List<EventoStaffDTO>> listarEventosDelStaff(@PathVariable Long staffUsuarioId) {
        return ResponseEntity.ok(inscripcionService.listarEventosDelStaff(staffUsuarioId));
    }

    // Convierte el texto de función a enum; si viene mal, lanza error 400.
    private static FuncionStaff parseFuncion(String texto) {
        if (texto == null || texto.isBlank()) return FuncionStaff.GENERAL;
        try {
            return FuncionStaff.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(
                    "Función no válida. Use CHECK_IN_QR, CHECK_IN_MANUAL, REGISTRO_SALIDA o GENERAL.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    // Lista asistentes del evento para que el staff haga check-in (puede filtrar con q). Devuelve lista de asistentes.
    @GetMapping("/api/eventos/{eventoId}/asistentes")
    public ResponseEntity<List<AsistenteEventoDTO>> listarAsistentesParaStaff(@PathVariable Long eventoId,
                                                                              @RequestParam Long staffUsuarioId,
                                                                              @RequestParam(required = false) String q) {
        return ResponseEntity.ok(inscripcionService.listarAsistentesParaStaff(eventoId, staffUsuarioId, q));
    }

    // Lista asistentes del evento para el organizador (vista de gestión). Devuelve lista de asistentes.
    @GetMapping("/api/eventos/{eventoId}/asistentes/organizador")
    public ResponseEntity<List<AsistenteEventoDTO>> listarAsistentesParaOrganizador(@PathVariable Long eventoId,
                                                                                    @RequestParam Long organizadorId,
                                                                                    @RequestParam(required = false) String q) {
        return ResponseEntity.ok(inscripcionService.listarAsistentesParaOrganizador(eventoId, organizadorId, q));
    }

    // Consulta cuánta gente hay dentro del evento en tiempo real. Devuelve AforoEnVivoDTO.
    @GetMapping("/api/eventos/{eventoId}/aforo")
    public ResponseEntity<AforoEnVivoDTO> aforoEnVivo(@PathVariable Long eventoId) {
        return ResponseEntity.ok(inscripcionService.consultarAforoEnVivo(eventoId));
    }
}
