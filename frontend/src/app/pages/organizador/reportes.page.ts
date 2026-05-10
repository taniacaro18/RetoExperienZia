import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { DialogModule } from 'primeng/dialog';
import { MessageService } from 'primeng/api';
import { AuthStore } from '../../core/auth/auth.store';
import { EventoApi } from '../../core/api/evento.api';
import { ReporteApi, ReporteEventoAvanzado } from '../../core/api/reporte.api';
import { Evento } from '../../core/models/domain.models';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';
import { DonutComponent } from '../../shared/donut/donut.component';
import { ExportService } from '../../core/export/export.service';
import * as XLSX from 'xlsx';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

@Component({
  selector: 'app-org-reportes-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    SelectModule,
    TagModule,
    ProgressSpinnerModule,
    DialogModule,
    StatCardComponent,
    DonutComponent
  ],
  templateUrl: './reportes.page.html'
})
export class OrgReportesPage {
  private readonly store = inject(AuthStore);
  private readonly route = inject(ActivatedRoute);
  private readonly eventoApi = inject(EventoApi);
  private readonly reporteApi = inject(ReporteApi);
  private readonly messages = inject(MessageService);
  private readonly exportSvc = inject(ExportService);

  readonly eventos = signal<Evento[]>([]);
  readonly cargando = signal(true);
  readonly cargandoReporte = signal(false);
  readonly eventoSeleccionado = signal<number | null>(null);
  readonly reporte = signal<ReporteEventoAvanzado | null>(null);

  readonly opciones = computed(() =>
    this.eventos()
      .filter((e) => e.estado === 'ACTIVO' || e.estado === 'FINALIZADO')
      .sort((a, b) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime())
      .map((e) => ({
        label: e.nombre + ' · ' + new Date(e.fecha).toLocaleDateString(),
        value: e.id
      }))
  );

  readonly maxIngresos = computed(() => {
    const r = this.reporte();
    if (!r || r.curvaIngreso.length === 0) return 1;
    return Math.max(1, ...r.curvaIngreso.map((p) => Math.max(p.ingresos, p.salidas)));
  });

  readonly maxStaff = computed(() => {
    const r = this.reporte();
    if (!r || r.desempenoStaff.length === 0) return 1;
    return Math.max(1, ...r.desempenoStaff.map((s) => s.checkInsRegistrados + s.checkOutsRegistrados));
  });

  readonly donutAsistencia = computed(() => {
    const r = this.reporte();
    if (!r) return [];
    return [
      { label: 'Asistieron', valor: r.asistieron, color: '#10B981' },
      { label: 'Faltaron', valor: r.faltaron, color: '#EF6C4D' }
    ];
  });

  readonly donutCheckIn = computed(() => {
    const r = this.reporte();
    if (!r) return [];
    return [
      { label: 'QR', valor: r.checkInsPorQR, color: '#7C49AE' },
      { label: 'Manual', valor: r.checkInsManuales, color: '#A77FD3' }
    ];
  });

  ngOnInit() {
    const orgId = this.store.usuario()?.id;
    if (!orgId) return;
    this.eventoApi.listarPorOrganizador(orgId).subscribe({
      next: (lista) => {
        this.eventos.set(lista);
        this.cargando.set(false);
        const desdeQuery = Number(this.route.snapshot.queryParamMap.get('evento'));
        const valido = lista.find((e) => e.id === desdeQuery);
        if (valido) {
          this.cambiarEvento(valido.id);
        } else if (this.opciones().length > 0) {
          this.cambiarEvento(this.opciones()[0].value);
        }
      },
      error: () => this.cargando.set(false)
    });
  }

  cambiarEvento(id: number) {
    const orgId = this.store.usuario()?.id;
    if (!orgId) return;
    this.eventoSeleccionado.set(id);
    this.cargandoReporte.set(true);
    this.reporteApi.reporteAvanzado(id, orgId).subscribe({
      next: (r) => {
        this.reporte.set(r);
        this.cargandoReporte.set(false);
      },
      error: () => {
        this.reporte.set(null);
        this.cargandoReporte.set(false);
      }
    });
  }

  porcentajeAsistenciaSalida(r: ReporteEventoAvanzado): number {
    return r.checkInsTotal === 0 ? 0 : Math.round((r.checkOutsTotal / r.checkInsTotal) * 100);
  }

  formatearHora(h: number): string {
    const hora = h % 24;
    return hora.toString().padStart(2, '0') + ':00';
  }

  funcionLabel(f: string): string {
    switch (f) {
      case 'CHECK_IN_QR': return 'Check-in / QR';
      case 'CHECK_IN_MANUAL': return 'Check-in Manual';
      case 'REGISTRO_SALIDA': return 'Registro Salida';
      case 'GENERAL': return 'General';
      default: return f;
    }
  }

  descargarExcel() {
    const r = this.reporte();
    if (!r) return;
    const wb = XLSX.utils.book_new();

    const resumen = [
      ['Reporte avanzado'],
      ['Evento', r.nombreEvento],
      ['Fecha', r.fechaEvento],
      ['Aforo máximo', r.aforoMaximo],
      ['Inscritos', r.inscritos],
      ['Ocupación %', Number(r.porcentajeOcupacion.toFixed(2))],
      ['Asistieron', r.asistieron],
      ['Faltaron', r.faltaron],
      ['Tasa asistencia %', Number(r.porcentajeAsistencia.toFixed(2))],
      ['Check-ins total', r.checkInsTotal],
      ['Check-ins QR', r.checkInsPorQR],
      ['Check-ins manuales', r.checkInsManuales],
      ['Check-outs total', r.checkOutsTotal]
    ];
    const wsResumen = XLSX.utils.aoa_to_sheet(resumen);
    wsResumen['!cols'] = [{ wch: 24 }, { wch: 30 }];
    XLSX.utils.book_append_sheet(wb, wsResumen, 'Resumen');

    const curva = [['Hora', 'Ingresos', 'Salidas']];
    r.curvaIngreso.forEach((p) => curva.push([this.formatearHora(p.hora), String(p.ingresos), String(p.salidas)]));
    const wsCurva = XLSX.utils.aoa_to_sheet(curva);
    wsCurva['!cols'] = [{ wch: 10 }, { wch: 12 }, { wch: 12 }];
    XLSX.utils.book_append_sheet(wb, wsCurva, 'Curva');

    const staff = [['Nombre', 'Función', 'Check-ins', 'Check-outs', 'QR', 'Manual']];
    r.desempenoStaff.forEach((s) =>
      staff.push([s.nombre ?? '—', this.funcionLabel(s.funcion),
        String(s.checkInsRegistrados), String(s.checkOutsRegistrados),
        String(s.checkInsPorQR), String(s.checkInsManuales)])
    );
    const wsStaff = XLSX.utils.aoa_to_sheet(staff);
    wsStaff['!cols'] = [{ wch: 28 }, { wch: 18 }, { wch: 12 }, { wch: 12 }, { wch: 8 }, { wch: 10 }];
    XLSX.utils.book_append_sheet(wb, wsStaff, 'Staff');

    XLSX.writeFile(wb, `reporte_${r.nombreEvento.replace(/\s+/g, '_')}.xlsx`);
    this.messages.add({ severity: 'success', summary: 'Excel descargado' });
  }

  descargarPdf() {
    const r = this.reporte();
    if (!r) return;
    const doc = new jsPDF({ orientation: 'portrait', unit: 'pt', format: 'a4' });
    doc.setFontSize(16);
    doc.setTextColor(60, 38, 121);
    doc.text('Reporte avanzado · ' + r.nombreEvento, 40, 40);
    doc.setFontSize(10);
    doc.setTextColor(100);
    doc.text('Fecha del evento: ' + r.fechaEvento, 40, 58);
    doc.text('Generado: ' + new Date().toLocaleString(), 40, 72);

    autoTable(doc, {
      startY: 90,
      head: [['Indicador', 'Valor']],
      body: [
        ['Aforo máximo', String(r.aforoMaximo)],
        ['Inscritos', String(r.inscritos)],
        ['Ocupación %', r.porcentajeOcupacion.toFixed(1) + '%'],
        ['Asistieron', String(r.asistieron)],
        ['Faltaron', String(r.faltaron)],
        ['Tasa de asistencia', r.porcentajeAsistencia.toFixed(1) + '%'],
        ['Check-ins total', String(r.checkInsTotal)],
        ['Check-ins por QR', String(r.checkInsPorQR)],
        ['Check-ins manuales', String(r.checkInsManuales)],
        ['Check-outs total', String(r.checkOutsTotal)]
      ],
      headStyles: { fillColor: [124, 99, 196], textColor: 255 },
      alternateRowStyles: { fillColor: [248, 244, 255] },
      styles: { fontSize: 9 }
    });

    const yCurva = (doc as any).lastAutoTable.finalY + 16;
    doc.setFontSize(12);
    doc.setTextColor(60, 38, 121);
    doc.text('Curva de ingresos / salidas', 40, yCurva);
    autoTable(doc, {
      startY: yCurva + 6,
      head: [['Hora', 'Ingresos', 'Salidas']],
      body: r.curvaIngreso.map((p) => [this.formatearHora(p.hora), String(p.ingresos), String(p.salidas)]),
      headStyles: { fillColor: [124, 99, 196], textColor: 255 },
      alternateRowStyles: { fillColor: [248, 244, 255] },
      styles: { fontSize: 9 }
    });

    const yStaff = (doc as any).lastAutoTable.finalY + 16;
    doc.setFontSize(12);
    doc.setTextColor(60, 38, 121);
    doc.text('Desempeño del staff', 40, yStaff);
    autoTable(doc, {
      startY: yStaff + 6,
      head: [['Nombre', 'Función', 'Check-ins', 'Check-outs', 'QR', 'Manual']],
      body: r.desempenoStaff.map((s) => [
        s.nombre ?? '—', this.funcionLabel(s.funcion),
        String(s.checkInsRegistrados), String(s.checkOutsRegistrados),
        String(s.checkInsPorQR), String(s.checkInsManuales)
      ]),
      headStyles: { fillColor: [124, 99, 196], textColor: 255 },
      alternateRowStyles: { fillColor: [248, 244, 255] },
      styles: { fontSize: 9 }
    });

    doc.save(`reporte_${r.nombreEvento.replace(/\s+/g, '_')}.pdf`);
    this.messages.add({ severity: 'success', summary: 'PDF descargado' });
  }
}
