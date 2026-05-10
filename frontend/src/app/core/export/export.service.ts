import { Injectable } from '@angular/core';
import * as XLSX from 'xlsx';
import jsPDF from 'jspdf';
import autoTable, { RowInput } from 'jspdf-autotable';

export interface ColumnaExport<T> {
  /** Encabezado humano que se muestra en Excel y PDF. */
  header: string;
  /** Función que extrae el valor de una fila. */
  value: (row: T) => string | number | null | undefined;
}

/**
 * Servicio centralizado para exportar listados a Excel (.xlsx) y PDF.
 * También sirve para descargar plantillas .xlsx con encabezados y filas de ejemplo.
 */
@Injectable({ providedIn: 'root' })
export class ExportService {
  /** Exporta un listado a un archivo .xlsx. */
  exportarExcel<T>(filename: string, sheetName: string, columnas: ColumnaExport<T>[], rows: T[]) {
    const headers = columnas.map((c) => c.header);
    const data = rows.map((r) => columnas.map((c) => c.value(r) ?? ''));
    const ws = XLSX.utils.aoa_to_sheet([headers, ...data]);

    const widths: XLSX.ColInfo[] = headers.map((h, i) => {
      const maxLen = Math.max(
        h.length,
        ...data.map((row) => String(row[i] ?? '').length)
      );
      return { wch: Math.min(40, Math.max(10, maxLen + 2)) };
    });
    ws['!cols'] = widths;

    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, sheetName.substring(0, 31));
    XLSX.writeFile(wb, this.asegurarExt(filename, 'xlsx'));
  }

  /**
   * Genera una plantilla .xlsx con encabezados obligatorios y, opcionalmente,
   * filas de ejemplo. Usado para carga masiva (ej. asistentes).
   */
  descargarPlantillaExcel(filename: string, sheetName: string,
                          headers: string[], filasEjemplo: (string | number)[][] = []) {
    const ws = XLSX.utils.aoa_to_sheet([headers, ...filasEjemplo]);
    ws['!cols'] = headers.map((h) => ({ wch: Math.max(14, h.length + 2) }));
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, sheetName.substring(0, 31));
    XLSX.writeFile(wb, this.asegurarExt(filename, 'xlsx'));
  }

  /**
   * Lee un archivo .xlsx (o .csv) y devuelve un array de objetos usando la primera
   * fila como encabezados. Si llega un .csv lo procesa también.
   */
  async leerExcelOCsv(archivo: File): Promise<Record<string, string>[]> {
    const buffer = await archivo.arrayBuffer();
    const wb = XLSX.read(buffer, { type: 'array' });
    const sheet = wb.Sheets[wb.SheetNames[0]];
    return XLSX.utils.sheet_to_json<Record<string, string>>(sheet, {
      defval: '',
      raw: false
    });
  }

  /** Convierte cualquier listado tabular a CSV (texto). Útil cuando el backend solo acepta CSV. */
  filasACsv(headers: string[], filas: (string | number | null | undefined)[][]): string {
    const escapar = (v: string | number | null | undefined): string => {
      const s = v == null ? '' : String(v);
      if (/[",\n;]/.test(s)) return `"${s.replace(/"/g, '""')}"`;
      return s;
    };
    const lineas = [headers.map(escapar).join(',')];
    filas.forEach((row) => lineas.push(row.map(escapar).join(',')));
    return lineas.join('\n');
  }

  /** Exporta a PDF usando jspdf-autotable. */
  exportarPdf<T>(filename: string, titulo: string,
                 columnas: ColumnaExport<T>[], rows: T[],
                 subtitulo?: string) {
    const doc = new jsPDF({ orientation: 'landscape', unit: 'pt', format: 'a4' });

    doc.setFontSize(16);
    doc.setTextColor(60, 38, 121);
    doc.text(titulo, 40, 40);
    if (subtitulo) {
      doc.setFontSize(10);
      doc.setTextColor(100);
      doc.text(subtitulo, 40, 58);
    }
    doc.setFontSize(8);
    doc.setTextColor(140);
    doc.text('Generado: ' + new Date().toLocaleString(), 40, subtitulo ? 72 : 58);

    const head = [columnas.map((c) => c.header)];
    const body: RowInput[] = rows.map((r) => columnas.map((c) => {
      const v = c.value(r);
      return v == null ? '' : String(v);
    }));

    autoTable(doc, {
      head,
      body,
      startY: subtitulo ? 88 : 78,
      styles: { fontSize: 8, cellPadding: 4 },
      headStyles: { fillColor: [124, 99, 196], textColor: 255, fontStyle: 'bold' },
      alternateRowStyles: { fillColor: [248, 244, 255] },
      margin: { left: 40, right: 40 }
    });

    doc.save(this.asegurarExt(filename, 'pdf'));
  }

  private asegurarExt(filename: string, ext: 'xlsx' | 'pdf'): string {
    return filename.toLowerCase().endsWith('.' + ext) ? filename : `${filename}.${ext}`;
  }
}
