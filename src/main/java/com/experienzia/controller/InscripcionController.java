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

@RestController
public class InscripcionController {

    private final InscripcionService inscripcionService;
    private final AuditoriaService auditoriaService;

    public InscripcionController(InscripcionService inscripcionService, AuditoriaService auditoriaService) {
        this.inscripcionService = inscripcionService;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping("/api/inscripciones")
    public ResponseEntity<InscripcionDTO> crear(@RequestBody InscripcionDTO dto, HttpServletRequest request) {
        InscripcionDTO ins = inscripcionService.inscribir(dto.getUsuarioId(), dto.getEventoId());
        auditoriaService.registrar(ins.getUsuarioId(), "INSCRIPCION_CREADA", "Inscripcion", ins.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ins);
    }

    @PutMapping("/api/inscripciones/{id}/cancelar")
    public ResponseEntity<InscripcionDTO> cancelar(@PathVariable Long id, HttpServletRequest request) {
        InscripcionDTO ins = inscripcionService.cancelar(id);
        auditoriaService.registrar(ins.getUsuarioId(), "INSCRIPCION_CANCELADA", "Inscripcion", ins.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(ins);
    }

    @GetMapping("/api/inscripciones/evento/{eventoId}")
    public ResponseEntity<List<InscripcionDTO>> listarPorEvento(@PathVariable Long eventoId) {
        return ResponseEntity.ok(inscripcionService.listarPorEvento(eventoId));
    }

    @GetMapping("/api/inscripciones/usuario/{usuarioId}")
    public ResponseEntity<List<InscripcionDTO>> listarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(inscripcionService.listarPorUsuario(usuarioId));
    }

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

    /** HU-015: check-in escaneando el código QR del asistente. */
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

    /** HU-017: check-out escaneando el código QR del asistente. */
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

    /** Listado plano (compatibilidad anterior). */
    @GetMapping("/api/eventos/{eventoId}/staff/ids")
    public ResponseEntity<List<Long>> listarStaffIds(@PathVariable Long eventoId) {
        return ResponseEntity.ok(inscripcionService.listarStaffIdsPorEvento(eventoId));
    }

    /** Listado enriquecido del staff con su función (vista del organizador). */
    @GetMapping("/api/eventos/{eventoId}/staff")
    public ResponseEntity<List<StaffAsignadoDTO>> listarStaffAsignados(@PathVariable Long eventoId) {
        return ResponseEntity.ok(inscripcionService.listarStaffPorEvento(eventoId));
    }

    /** Eventos asignados al usuario STAFF (vista del staff). */
    @GetMapping("/api/staff/{staffUsuarioId}/eventos")
    public ResponseEntity<List<EventoStaffDTO>> listarEventosDelStaff(@PathVariable Long staffUsuarioId) {
        return ResponseEntity.ok(inscripcionService.listarEventosDelStaff(staffUsuarioId));
    }

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

    @GetMapping("/api/eventos/{eventoId}/asistentes")
    public ResponseEntity<List<AsistenteEventoDTO>> listarAsistentesParaStaff(@PathVariable Long eventoId,
                                                                              @RequestParam Long staffUsuarioId,
                                                                              @RequestParam(required = false) String q) {
        return ResponseEntity.ok(inscripcionService.listarAsistentesParaStaff(eventoId, staffUsuarioId, q));
    }

    @GetMapping("/api/eventos/{eventoId}/asistentes/organizador")
    public ResponseEntity<List<AsistenteEventoDTO>> listarAsistentesParaOrganizador(@PathVariable Long eventoId,
                                                                                    @RequestParam Long organizadorId,
                                                                                    @RequestParam(required = false) String q) {
        return ResponseEntity.ok(inscripcionService.listarAsistentesParaOrganizador(eventoId, organizadorId, q));
    }

    @GetMapping("/api/eventos/{eventoId}/aforo")
    public ResponseEntity<AforoEnVivoDTO> aforoEnVivo(@PathVariable Long eventoId) {
        return ResponseEntity.ok(inscripcionService.consultarAforoEnVivo(eventoId));
    }
}
