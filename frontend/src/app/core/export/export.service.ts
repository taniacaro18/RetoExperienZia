import { Injectable } from '@angular/core';
import * as XLSX from 'xlsx';
import jsPDF from 'jspdf';
import autoTable, { RowInput } from 'jspdf-autotable';
import type { ReporteEventoAvanzado } from '../api/reporte.api';

/** Alineación con las vistas previas en pantalla (cabeceras KPI distintas). */
export type ReporteVistaPrevia = 'admin' | 'organizador';

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

  /**
   * Excel: primera hoja replica la vista previa; las siguientes conservan tablas para análisis.
   */
  exportarReporteAvanzadoExcel(
    filenameBase: string,
    r: ReporteEventoAvanzado,
    vista: ReporteVistaPrevia
  ): void {
    const wb = XLSX.utils.book_new();
    const vistaPreviaAoA = this.construirHojaVistaPreviaAoA(r, vista);
    const wsVista = XLSX.utils.aoa_to_sheet(vistaPreviaAoA);
    wsVista['!cols'] = [{ wch: 28 }, { wch: 16 }, { wch: 16 }, { wch: 16 }];
    XLSX.utils.book_append_sheet(wb, wsVista, 'Vista previa');

    const curva: (string | number)[][] = [['Hora', 'Ingresos', 'Salidas']];
    for (const p of r.curvaIngreso) {
      curva.push([this.formatearHoraCurva(p.hora), p.ingresos, p.salidas]);
    }
    const wsCurva = XLSX.utils.aoa_to_sheet(curva);
    wsCurva['!cols'] = [{ wch: 10 }, { wch: 12 }, { wch: 12 }];
    XLSX.utils.book_append_sheet(wb, wsCurva, 'Curva horaria');

    const staff: (string | number)[][] = [
      ['Nombre', 'Función', 'Check-ins', 'Check-outs', 'QR', 'Manual']
    ];
    for (const s of r.desempenoStaff) {
      staff.push([
        s.nombre ?? '—',
        this.funcionStaffLabel(s.funcion),
        s.checkInsRegistrados,
        s.checkOutsRegistrados,
        s.checkInsPorQR,
        s.checkInsManuales
      ]);
    }
    const wsStaff = XLSX.utils.aoa_to_sheet(staff);
    wsStaff['!cols'] = [{ wch: 28 }, { wch: 18 }, { wch: 12 }, { wch: 12 }, { wch: 8 }, { wch: 10 }];
    XLSX.utils.book_append_sheet(wb, wsStaff, 'Staff detalle');

    XLSX.writeFile(wb, this.asegurarExt(this.slugArchivo(filenameBase), 'xlsx'));
  }

  /**
   * PDF: misma estructura que la vista previa del rol en la 1.ª página.
   * Organizador: 2.ª página anexo con curva horaria y staff detallado (como en Excel).
   */
  exportarReporteAvanzadoPdf(
    filenameBase: string,
    r: ReporteEventoAvanzado,
    vista: ReporteVistaPrevia
  ): void {
    const doc = new jsPDF({ orientation: 'portrait', unit: 'pt', format: 'a4' });
    const Margen = 40;
    const AnchoUtil = 515;

    doc.setFontSize(9);
    doc.setTextColor(120);
    doc.text('Generado: ' + new Date().toLocaleString(), Margen, 26);

    let y = this.dibujarEncabezadoEventoPdf(doc, r, Margen, 38, AnchoUtil);
    y += 12;

    if (vista === 'admin') {
      autoTable(doc, {
        startY: y,
        head: [['Inscritos', 'Asistieron', 'Faltaron', 'Ocupación %']],
        body: [
          [
            String(r.inscritos),
            String(r.asistieron),
            String(r.faltaron),
            r.porcentajeOcupacion.toFixed(1) + '%'
          ]
        ],
        styles: { fontSize: 10, cellPadding: 8, halign: 'center' },
        headStyles: { fillColor: [237, 233, 250], textColor: 60 },
        alternateRowStyles: { fillColor: [255, 255, 255] },
        margin: { left: Margen, right: Margen }
      });
    } else {
      autoTable(doc, {
        startY: y,
        head: [['Asistencia %', 'Check-ins', 'Check-outs', 'Ocupación %']],
        body: [
          [
            r.porcentajeAsistencia.toFixed(1) + '%',
            String(r.checkInsTotal),
            String(r.checkOutsTotal),
            r.porcentajeOcupacion.toFixed(1) + '%'
          ]
        ],
        styles: { fontSize: 10, cellPadding: 8, halign: 'center' },
        headStyles: { fillColor: [237, 233, 250], textColor: 60 },
        margin: { left: Margen, right: Margen }
      });
    }

    y = this.finalY(doc) + 18;
    doc.setFontSize(11);
    doc.setTextColor(60, 38, 121);
    doc.text('Asistencia', Margen, y);
    y = this.dibujarBloqueDonutBarrasPdf(doc, Margen, y + 8, AnchoUtil, [
      {
        texto: `Asistieron: ${r.asistieron}  (${this.porcion(r.asistieron, Math.max(r.inscritos, 1))}% sobre inscritos)`,
        rgb: [16, 185, 129] as const,
        pct: this.porcionNum(r.asistieron, Math.max(r.inscritos, 1))
      },
      {
        texto: `Faltaron: ${r.faltaron}  (${this.porcion(r.faltaron, Math.max(r.inscritos, 1))}%)`,
        rgb: [248, 113, 113] as const,
        pct: this.porcionNum(r.faltaron, Math.max(r.inscritos, 1))
      }
    ]);

    if (y > 640) {
      doc.addPage();
      y = Margen;
    }

    doc.setFontSize(11);
    doc.setTextColor(60, 38, 121);
    doc.text('Tipo de check-in', Margen, y);
    const denomCi = Math.max(r.checkInsPorQR + r.checkInsManuales, r.checkInsTotal, 1);
    y = this.dibujarBloqueDonutBarrasPdf(doc, Margen, y + 8, AnchoUtil, [
      {
        texto: `Por QR: ${r.checkInsPorQR}  (${this.porcion(r.checkInsPorQR, denomCi)}%)`,
        rgb: [124, 58, 237] as const,
        pct: this.porcionNum(r.checkInsPorQR, denomCi)
      },
      {
        texto: `Manuales: ${r.checkInsManuales}  (${this.porcion(r.checkInsManuales, denomCi)}%)`,
        rgb: [167, 127, 211] as const,
        pct: this.porcionNum(r.checkInsManuales, denomCi)
      }
    ]);

    if (vista === 'admin') {
      if (y > 520) {
        doc.addPage();
        y = Margen;
      }
      doc.setFontSize(11);
      doc.setTextColor(60, 38, 121);
      doc.text('Curva horaria', Margen, y);
      y += 8;
      this.dibujarMiniCurvaPdf(doc, Margen, y, r, AnchoUtil, 88);
      y += 88 + 24;
      doc.setFontSize(7);
      doc.setTextColor(80);
      doc.text('Verde = ingresos · Coral = salidas', Margen, y);
      y += 16;

      if (r.desempenoStaff.length > 0) {
        if (y > 630) {
          doc.addPage();
          y = Margen;
        }
        doc.setFontSize(11);
        doc.setTextColor(60, 38, 121);
        doc.text('Desempeño del staff', Margen, y);
        y += 10;
        autoTable(doc, {
          startY: y,
          head: [['Staff', 'CI', 'CO']],
          body: r.desempenoStaff.map((s) => [
            s.nombre ?? '#' + s.staffUsuarioId,
            String(s.checkInsRegistrados),
            String(s.checkOutsRegistrados)
          ]),
          styles: { fontSize: 9 },
          headStyles: { fillColor: [124, 99, 196], textColor: 255 },
          alternateRowStyles: { fillColor: [248, 244, 255] },
          margin: { left: Margen, right: Margen }
        });
      }
    }

    if (vista === 'organizador') {
      this.agregarPaginaAnexoOrganizadorPdf(doc, r, Margen, AnchoUtil);
    }

    doc.save(this.asegurarExt(this.slugArchivo(filenameBase), 'pdf'));
  }

  /** Segunda página solo para PDF de organizador: curva + staff completo. */
  private agregarPaginaAnexoOrganizadorPdf(
    doc: jsPDF,
    r: ReporteEventoAvanzado,
    Margen: number,
    AnchoUtil: number
  ): void {
    doc.addPage();
    doc.setFontSize(9);
    doc.setTextColor(120);
    doc.text('Generado: ' + new Date().toLocaleString(), Margen, 26);

    doc.setFontSize(13);
    doc.setTextColor(60, 38, 121);
    doc.text('Anexo · detalle operativo', Margen, 48);
    doc.setFontSize(9);
    doc.setTextColor(90);
    const sub = doc.splitTextToSize(`${r.nombreEvento} · ${r.fechaEvento}`, AnchoUtil);
    let y = 62;
    for (const ln of sub) {
      doc.text(ln, Margen, y);
      y += 11;
    }
    y += 8;

    doc.setFontSize(11);
    doc.setTextColor(60, 38, 121);
    doc.text('Curva horaria', Margen, y);
    y += 10;
    this.dibujarMiniCurvaPdf(doc, Margen, y, r, AnchoUtil, 88);
    y += 88 + 18;
    doc.setFontSize(7);
    doc.setTextColor(80);
    doc.text('Verde = ingresos · Coral = salidas', Margen, y);
    y += 22;

    if (r.desempenoStaff.length === 0) {
      doc.setFontSize(9);
      doc.setTextColor(120);
      doc.text('Sin registros de staff para este evento.', Margen, y);
      return;
    }

    if (y > 520) {
      doc.addPage();
      y = Margen + 20;
    }

    doc.setFontSize(11);
    doc.setTextColor(60, 38, 121);
    doc.text('Desempeño del staff (detalle)', Margen, y);
    y += 10;
    autoTable(doc, {
      startY: y,
      head: [['Nombre', 'Función', 'Check-ins', 'Check-outs', 'QR', 'Manual']],
      body: r.desempenoStaff.map((s) => [
        s.nombre ?? '—',
        this.funcionStaffLabel(s.funcion),
        String(s.checkInsRegistrados),
        String(s.checkOutsRegistrados),
        String(s.checkInsPorQR),
        String(s.checkInsManuales)
      ]),
      styles: { fontSize: 8 },
      headStyles: { fillColor: [124, 99, 196], textColor: 255 },
      alternateRowStyles: { fillColor: [248, 244, 255] },
      margin: { left: Margen, right: Margen }
    });
  }

  private construirHojaVistaPreviaAoA(r: ReporteEventoAvanzado, vista: ReporteVistaPrevia): (string | number)[][] {
    const rows: (string | number)[][] = [
      ['Reporte avanzado · ExperienZia · Vista previa'],
      [],
      ['Evento', r.nombreEvento],
      ['Fecha', r.fechaEvento],
      ['Aforo máximo', r.aforoMaximo],
      []
    ];
    if (vista === 'admin') {
      rows.push(['Inscritos', 'Asistieron', 'Faltaron', 'Ocupación %']);
      rows.push([
        r.inscritos,
        r.asistieron,
        r.faltaron,
        Number(r.porcentajeOcupacion.toFixed(2))
      ]);
    } else {
      rows.push(['Asistencia %', 'Check-ins totales', 'Check-outs totales', 'Ocupación %']);
      rows.push([
        Number(r.porcentajeAsistencia.toFixed(2)),
        r.checkInsTotal,
        r.checkOutsTotal,
        Number(r.porcentajeOcupacion.toFixed(2))
      ]);
    }
    rows.push([], ['— Asistencia —'], ['Concepto', 'Cantidad', '% sobre inscritos']);
    const insc = Math.max(r.inscritos, 1);
    rows.push([
      'Asistieron',
      r.asistieron,
      Number(((100 * r.asistieron) / insc).toFixed(2))
    ]);
    rows.push([
      'Faltaron',
      r.faltaron,
      Number(((100 * r.faltaron) / insc).toFixed(2))
    ]);
    rows.push([], ['— Check-in —'], ['Tipo', 'Cantidad', '% sobre QR+manual']);
    const denomCi = Math.max(r.checkInsPorQR + r.checkInsManuales, r.checkInsTotal, 1);
    rows.push([
      'Por QR',
      r.checkInsPorQR,
      Number(((100 * r.checkInsPorQR) / denomCi).toFixed(2))
    ]);
    rows.push([
      'Manual',
      r.checkInsManuales,
      Number(((100 * r.checkInsManuales) / denomCi).toFixed(2))
    ]);
    if (vista === 'admin') {
      rows.push([], ['— Curva horaria (valores por hora) —']);
      rows.push(['Hora', 'Ingresos', 'Salidas']);
      for (const p of r.curvaIngreso) {
        rows.push([this.formatearHoraCurva(p.hora), p.ingresos, p.salidas]);
      }
      rows.push([], ['— Staff (igual vista previa) —']);
      rows.push(['Staff', 'CI', 'CO']);
      for (const s of r.desempenoStaff) {
        rows.push([
          s.nombre ?? '#' + s.staffUsuarioId,
          s.checkInsRegistrados,
          s.checkOutsRegistrados
        ]);
      }
    }
    return rows;
  }

  private dibujarEncabezadoEventoPdf(
    doc: jsPDF,
    r: ReporteEventoAvanzado,
    x: number,
    y: number,
    w: number
  ): number {
    const lineas = doc.splitTextToSize(r.nombreEvento, w - 20);
    const h = Math.max(48, 22 + lineas.length * 13 + 18);
    doc.setFillColor(124, 73, 174);
    doc.rect(x, y, w, h, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(12);
    let yy = y + 18;
    for (const ln of lineas) {
      doc.text(ln, x + 10, yy);
      yy += 13;
    }
    doc.setFontSize(9);
    doc.text(String(r.fechaEvento), x + 10, yy + 4);
    doc.setTextColor(0, 0, 0);
    return y + h;
  }

  private dibujarBloqueDonutBarrasPdf(
    doc: jsPDF,
    x: number,
    y: number,
    w: number,
    segmentos: { texto: string; rgb: readonly [number, number, number]; pct: number }[]
  ): number {
    const barH = 14;
    let xo = x;
    const totalPct = segmentos.reduce((a, b) => a + b.pct, 0) || 1;
    const factor = totalPct > 99.9 && totalPct < 100.1 ? 100 / totalPct : 1;
    for (const seg of segmentos) {
      const pw = Math.max((seg.pct * factor) / 100, 0) * w;
      doc.setFillColor(seg.rgb[0], seg.rgb[1], seg.rgb[2]);
      doc.rect(xo, y, pw, barH, 'F');
      xo += pw;
    }
    doc.setDrawColor(180);
    doc.rect(x, y, w, barH, 'S');
    let ty = y + barH + 12;
    doc.setFontSize(8);
    doc.setTextColor(70);
    for (const seg of segmentos) {
      doc.text(seg.texto, x, ty);
      ty += 11;
    }
    return ty + 6;
  }

  private porcion(parte: number, total: number): string {
    if (total <= 0) return '0';
    return ((100 * parte) / total).toFixed(1);
  }

  private porcionNum(parte: number, total: number): number {
    if (total <= 0) return 0;
    return (100 * parte) / total;
  }

  private finalY(doc: jsPDF): number {
    return (doc as unknown as { lastAutoTable?: { finalY?: number } }).lastAutoTable?.finalY ?? 80;
  }

  private dibujarMiniCurvaPdf(
    doc: jsPDF,
    x0: number,
    y0: number,
    r: ReporteEventoAvanzado,
    w = 515,
    h = 72
  ): void {
    const pts = r.curvaIngreso;
    const maxVal = Math.max(1, ...pts.map((p) => Math.max(p.ingresos, p.salidas)));
    const n = pts.length || 1;
    const bw = w / n;
    doc.setDrawColor(220);
    doc.rect(x0, y0, w, h);

    pts.forEach((p, i) => {
      const x = x0 + i * bw + 1;
      const hi = (p.ingresos / maxVal) * (h - 8);
      const hs = (p.salidas / maxVal) * (h - 8);
      doc.setFillColor(16, 185, 129);
      doc.rect(x, y0 + h - hi - 2, bw / 2 - 1.5, hi, 'F');
      doc.setFillColor(239, 108, 77);
      doc.rect(x + bw / 2, y0 + h - hs - 2, bw / 2 - 1.5, hs, 'F');
    });
  }

  private formatearHoraCurva(h: number): string {
    const hora = ((h % 24) + 24) % 24;
    return hora.toString().padStart(2, '0') + ':00';
  }

  private funcionStaffLabel(f: string): string {
    switch (f) {
      case 'CHECK_IN_QR':
        return 'Check-in QR';
      case 'CHECK_IN_MANUAL':
        return 'Check-in manual';
      case 'REGISTRO_SALIDA':
        return 'Registro salida';
      case 'GENERAL':
        return 'General';
      default:
        return f;
    }
  }

  private slugArchivo(base: string): string {
    const s = base
      .trim()
      .replace(/\s+/g, '_')
      .replace(/[<>:"/\\|?*\u0000-\u001f]/g, '')
      .slice(0, 120);
    return s || 'reporte';
  }

  private asegurarExt(filename: string, ext: 'xlsx' | 'pdf'): string {
    return filename.toLowerCase().endsWith('.' + ext) ? filename : `${filename}.${ext}`;
  }
}
