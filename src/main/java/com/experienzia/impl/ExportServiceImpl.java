package com.experienzia.impl;

import com.experienzia.dto.AsistenteEventoDTO;
import com.experienzia.dto.EventoDTO;
import com.experienzia.exceptions.CustomException;
import com.experienzia.service.export.ExportService;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Implementación de exportadores en backend.
 * <p>
 * Usa Apache POI para Excel (.xlsx) y OpenPDF (fork libre de iText 2.x) para PDF.
 * Los archivos quedan en memoria como {@code byte[]} y se entregan como descarga
 * desde los endpoints REST correspondientes.
 */
@Service
public class ExportServiceImpl implements ExportService {

    private static final DateTimeFormatter FECHA_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color BRAND = new Color(124, 99, 196);
    private static final Color BRAND_LIGHT = new Color(248, 244, 255);

    @Override
    public byte[] resumenAsistentesExcel(EventoDTO evento, List<AsistenteEventoDTO> asistentes) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = wb.createSheet("Asistentes");
            CellStyle estiloHeader = headerStyle(wb);

            String[] headers = {"Nombre", "Email", "Teléfono", "Tipo doc.", "Número doc.",
                    "Estado", "Fecha inscripción", "Check-in", "Check-out"};
            crearFilaHeaders(hoja, headers, estiloHeader);

            int fila = 1;
            for (AsistenteEventoDTO a : asistentes) {
                Row row = hoja.createRow(fila++);
                row.createCell(0).setCellValue(safe(a.getNombre()));
                row.createCell(1).setCellValue(safe(a.getEmail()));
                row.createCell(2).setCellValue(safe(a.getTelefono()));
                row.createCell(3).setCellValue(safe(a.getTipoDocumento()));
                row.createCell(4).setCellValue(safe(a.getNumeroDocumento()));
                row.createCell(5).setCellValue(safe(a.getEstadoInscripcion()));
                row.createCell(6).setCellValue(a.getFechaInscripcion() == null ? "" : a.getFechaInscripcion().format(FECHA_FMT));
                row.createCell(7).setCellValue(a.getFechaCheckIn() == null ? "" : a.getFechaCheckIn().format(FECHA_FMT));
                row.createCell(8).setCellValue(a.getFechaCheckOut() == null ? "" : a.getFechaCheckOut().format(FECHA_FMT));
            }

            for (int i = 0; i < headers.length; i++) hoja.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new CustomException("No se pudo generar el Excel: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public byte[] resumenAsistentesPdf(EventoDTO evento, List<AsistenteEventoDTO> asistentes) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 48, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font tituloF = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BRAND);
            Font subF = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

            Paragraph titulo = new Paragraph("Asistentes · " + safe(evento == null ? "" : evento.getNombre()), tituloF);
            doc.add(titulo);
            if (evento != null && evento.getFecha() != null) {
                doc.add(new Paragraph("Fecha del evento: " + evento.getFecha().format(FECHA_FMT), subF));
            }
            doc.add(new Paragraph("Total: " + asistentes.size() + " asistente(s)", subF));
            doc.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(new float[]{2.5f, 3.5f, 2f, 1.2f, 2f, 1.5f, 2f, 2f, 2f});
            tabla.setWidthPercentage(100);
            String[] headers = {"Nombre", "Email", "Teléfono", "Tipo", "Documento",
                    "Estado", "Inscripción", "Check-in", "Check-out"};
            for (String h : headers) tabla.addCell(headerCellPdf(h));

            boolean alt = false;
            for (AsistenteEventoDTO a : asistentes) {
                tabla.addCell(bodyCellPdf(safe(a.getNombre()), alt));
                tabla.addCell(bodyCellPdf(safe(a.getEmail()), alt));
                tabla.addCell(bodyCellPdf(safe(a.getTelefono()), alt));
                tabla.addCell(bodyCellPdf(safe(a.getTipoDocumento()), alt));
                tabla.addCell(bodyCellPdf(safe(a.getNumeroDocumento()), alt));
                tabla.addCell(bodyCellPdf(safe(a.getEstadoInscripcion()), alt));
                tabla.addCell(bodyCellPdf(a.getFechaInscripcion() == null ? "" : a.getFechaInscripcion().format(FECHA_FMT), alt));
                tabla.addCell(bodyCellPdf(a.getFechaCheckIn() == null ? "" : a.getFechaCheckIn().format(FECHA_FMT), alt));
                tabla.addCell(bodyCellPdf(a.getFechaCheckOut() == null ? "" : a.getFechaCheckOut().format(FECHA_FMT), alt));
                alt = !alt;
            }

            doc.add(tabla);
            doc.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new CustomException("No se pudo generar el PDF: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public byte[] eventosExcel(List<EventoDTO> eventos) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = wb.createSheet("Eventos");
            CellStyle estiloHeader = headerStyle(wb);

            String[] headers = {"ID", "Nombre", "Categoría", "Tipo", "Estado",
                    "Fecha", "Fecha fin", "Ubicación", "Aforo máx.", "Aforo actual",
                    "Costo", "Organizador ID"};
            crearFilaHeaders(hoja, headers, estiloHeader);

            int fila = 1;
            for (EventoDTO e : eventos) {
                Row row = hoja.createRow(fila++);
                row.createCell(0).setCellValue(e.getId() == null ? 0 : e.getId());
                row.createCell(1).setCellValue(safe(e.getNombre()));
                row.createCell(2).setCellValue(safe(e.getCategoria()));
                row.createCell(3).setCellValue(safe(e.getTipoEvento()));
                row.createCell(4).setCellValue(safe(e.getEstado()));
                row.createCell(5).setCellValue(e.getFecha() == null ? "" : e.getFecha().format(FECHA_FMT));
                row.createCell(6).setCellValue(e.getFechaFin() == null ? "" : e.getFechaFin().format(FECHA_FMT));
                row.createCell(7).setCellValue(safe(e.getUbicacion()));
                row.createCell(8).setCellValue(e.getAforoMaximo() == null ? 0 : e.getAforoMaximo());
                row.createCell(9).setCellValue(e.getAforoActual() == null ? 0 : e.getAforoActual());
                row.createCell(10).setCellValue(e.getCosto() == null ? 0d : e.getCosto());
                row.createCell(11).setCellValue(e.getOrganizadorId() == null ? 0 : e.getOrganizadorId());
            }
            for (int i = 0; i < headers.length; i++) hoja.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new CustomException("No se pudo generar el Excel: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public byte[] eventosPdf(List<EventoDTO> eventos) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 48, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font tituloF = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BRAND);
            Font subF = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

            doc.add(new Paragraph("Listado de eventos · ExperienZia", tituloF));
            doc.add(new Paragraph("Total: " + eventos.size() + " evento(s)", subF));
            doc.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(new float[]{0.7f, 3f, 1.5f, 1.5f, 1.5f, 2f, 2.5f, 1.2f, 1.5f});
            tabla.setWidthPercentage(100);
            String[] headers = {"ID", "Nombre", "Categoría", "Tipo", "Estado",
                    "Fecha", "Ubicación", "Aforo", "Costo"};
            for (String h : headers) tabla.addCell(headerCellPdf(h));

            boolean alt = false;
            for (EventoDTO e : eventos) {
                tabla.addCell(bodyCellPdf(String.valueOf(e.getId()), alt));
                tabla.addCell(bodyCellPdf(safe(e.getNombre()), alt));
                tabla.addCell(bodyCellPdf(safe(e.getCategoria()), alt));
                tabla.addCell(bodyCellPdf(safe(e.getTipoEvento()), alt));
                tabla.addCell(bodyCellPdf(safe(e.getEstado()), alt));
                tabla.addCell(bodyCellPdf(e.getFecha() == null ? "" : e.getFecha().format(FECHA_FMT), alt));
                tabla.addCell(bodyCellPdf(safe(e.getUbicacion()), alt));
                String aforo = (e.getAforoActual() == null ? 0 : e.getAforoActual()) + "/"
                        + (e.getAforoMaximo() == null ? 0 : e.getAforoMaximo());
                tabla.addCell(bodyCellPdf(aforo, alt));
                tabla.addCell(bodyCellPdf(e.getCosto() == null ? "" : String.format("$%,.0f", e.getCosto()), alt));
                alt = !alt;
            }

            doc.add(tabla);
            doc.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new CustomException("No se pudo generar el PDF: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }

    private void crearFilaHeaders(Sheet hoja, String[] headers, CellStyle estilo) {
        Row header = hoja.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = header.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(estilo);
        }
    }

    private PdfPCell headerCellPdf(String texto) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        PdfPCell c = new PdfPCell(new Phrase(texto, f));
        c.setBackgroundColor(BRAND);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(6f);
        c.setBorderColor(BRAND);
        c.setBorderWidth(0.5f);
        return c;
    }

    private PdfPCell bodyCellPdf(String texto, boolean alt) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
        PdfPCell c = new PdfPCell(new Phrase(texto == null ? "" : texto, f));
        c.setBackgroundColor(alt ? BRAND_LIGHT : Color.WHITE);
        c.setPadding(5f);
        c.setBorderColor(new Color(220, 215, 235));
        c.setBorderWidth(0.5f);
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        c.setVerticalAlignment(Rectangle.ALIGN_MIDDLE);
        return c;
    }

    private String safe(Object v) {
        return v == null ? "" : v.toString();
    }
}
