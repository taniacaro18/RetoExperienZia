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
    byte[] resumenAsistentesExcel(EventoDTO evento, List<AsistenteEventoDTO> asistentes);
    byte[] resumenAsistentesPdf(EventoDTO evento, List<AsistenteEventoDTO> asistentes);

    byte[] eventosExcel(List<EventoDTO> eventos);
    byte[] eventosPdf(List<EventoDTO> eventos);

    /** Certificado individual (nombre, curso/evento, duración, código) en PDF. */
    byte[] certificadoPdf(CertificadoDTO certificado);
}
