package com.experienzia.controller;

import com.experienzia.dto.AsistenteEventoDTO;
import com.experienzia.dto.EventoDTO;
import com.experienzia.service.EventoService;
import com.experienzia.service.InscripcionService;
import com.experienzia.service.export.ExportService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para descargar archivos Excel y PDF generados en el servidor.
 * URL base: /api/export
 * Lo usan ADMIN (todos los eventos) y ORGANIZADOR (asistentes de su evento).
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExportService exportService;
    private final EventoService eventoService;
    private final InscripcionService inscripcionService;

    public ExportController(ExportService exportService,
                            EventoService eventoService,
                            InscripcionService inscripcionService) {
        this.exportService = exportService;
        this.eventoService = eventoService;
        this.inscripcionService = inscripcionService;
    }

    // Descarga Excel con todos los eventos (admin). Devuelve archivo .xlsx como descarga.
    @GetMapping(value = "/eventos.xlsx")
    public ResponseEntity<ByteArrayResource> eventosExcel() {
        List<EventoDTO> lista = eventoService.listarTodos();
        byte[] data = exportService.eventosExcel(lista);
        return descarga(data, "eventos.xlsx", XLSX);
    }

    // Descarga PDF con todos los eventos (admin). Devuelve archivo .pdf como descarga.
    @GetMapping(value = "/eventos.pdf")
    public ResponseEntity<ByteArrayResource> eventosPdf() {
        List<EventoDTO> lista = eventoService.listarTodos();
        byte[] data = exportService.eventosPdf(lista);
        return descarga(data, "eventos.pdf", MediaType.APPLICATION_PDF);
    }

    // Descarga Excel con asistentes de un evento. Devuelve archivo .xlsx como descarga.
    @GetMapping(value = "/eventos/{eventoId}/asistentes.xlsx")
    public ResponseEntity<ByteArrayResource> asistentesExcel(
            @PathVariable Long eventoId,
            @RequestParam(required = false) Long organizadorId) {
        EventoDTO evento = eventoService.obtenerPorId(eventoId);
        List<AsistenteEventoDTO> asistentes = obtenerAsistentes(eventoId, organizadorId);
        byte[] data = exportService.resumenAsistentesExcel(evento, asistentes);
        String nombre = "asistentes_" + sanear(evento.getNombre()) + ".xlsx";
        return descarga(data, nombre, XLSX);
    }

    // Descarga PDF con asistentes de un evento. Devuelve archivo .pdf como descarga.
    @GetMapping(value = "/eventos/{eventoId}/asistentes.pdf")
    public ResponseEntity<ByteArrayResource> asistentesPdf(
            @PathVariable Long eventoId,
            @RequestParam(required = false) Long organizadorId) {
        EventoDTO evento = eventoService.obtenerPorId(eventoId);
        List<AsistenteEventoDTO> asistentes = obtenerAsistentes(eventoId, organizadorId);
        byte[] data = exportService.resumenAsistentesPdf(evento, asistentes);
        String nombre = "asistentes_" + sanear(evento.getNombre()) + ".pdf";
        return descarga(data, nombre, MediaType.APPLICATION_PDF);
    }

    // Obtiene la lista de asistentes según si viene organizadorId o no.
    private List<AsistenteEventoDTO> obtenerAsistentes(Long eventoId, Long organizadorId) {
        if (organizadorId != null) {
            return inscripcionService.listarAsistentesParaOrganizador(eventoId, organizadorId, null);
        }
        // Sin organizador: equivale a vista admin (sin filtro de propietario).
        // Reutilizamos el método con staff null: el endpoint requiere acceso de organizador,
        // así que para admin recomendamos pasar organizadorId del dueño del evento.
        // Como fallback, devolvemos vacío para no exponer datos sin contexto.
        return List.of();
    }

    // Arma la respuesta HTTP para que el navegador descargue el archivo.
    private ResponseEntity<ByteArrayResource> descarga(byte[] data, String filename, MediaType tipo) {
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentLength(data.length)
                .body(resource);
    }

    // Limpia el nombre del evento para usarlo en el nombre del archivo.
    private String sanear(String nombre) {
        if (nombre == null || nombre.isBlank()) return "evento";
        return nombre.replaceAll("[^A-Za-z0-9_-]+", "_");
    }
}
