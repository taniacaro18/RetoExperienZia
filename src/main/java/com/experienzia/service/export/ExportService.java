package com.experienzia.service.export;

import com.experienzia.dto.AsistenteEventoDTO;
import com.experienzia.dto.CertificadoDTO;
import com.experienzia.dto.EventoDTO;

import java.util.List;

/**
 * Genera Excel (.xlsx) y PDF nativos en el servidor para los reportes y
 * listados administrativos. Reutiliza Apache POI y OpenPDF.
 */
public interface ExportService {

    /** Excel con la lista de asistentes de un evento. */
    byte[] resumenAsistentesExcel(EventoDTO evento, List<AsistenteEventoDTO> asistentes);

    /** PDF con la lista de asistentes de un evento. */
    byte[] resumenAsistentesPdf(EventoDTO evento, List<AsistenteEventoDTO> asistentes);

    /** Excel con listado de eventos. */
    byte[] eventosExcel(List<EventoDTO> eventos);

    /** PDF con listado de eventos. */
    byte[] eventosPdf(List<EventoDTO> eventos);

    /** Certificado individual (nombre, curso/evento, duración, código) en PDF. */
    byte[] certificadoPdf(CertificadoDTO certificado);
}
