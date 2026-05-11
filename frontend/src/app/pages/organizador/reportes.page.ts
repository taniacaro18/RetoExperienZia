import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
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

@Component({
  selector: 'app-org-reportes-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
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
  readonly vistaPreviaVisible = signal(false);

  readonly filtroTipoEvento = signal<'TODOS' | 'PUBLICO' | 'PRIVADO'>('TODOS');
  readonly fechaDesde = signal('');
  readonly fechaHasta = signal('');

  readonly opciones = computed(() => {
    let list = this.eventos().filter((e) => e.estado === 'ACTIVO' || e.estado === 'FINALIZADO');
    const ft = this.filtroTipoEvento();
    if (ft !== 'TODOS') list = list.filter((e) => e.tipoEvento === ft);
    const d1 = this.fechaDesde();
    const d2 = this.fechaHasta();
    if (d1) {
      const t = new Date(d1);
      t.setHours(0, 0, 0, 0);
      list = list.filter((e) => new Date(e.fecha).getTime() >= t.getTime());
    }
    if (d2) {
      const t = new Date(d2);
      t.setHours(23, 59, 59, 999);
      list = list.filter((e) => new Date(e.fecha).getTime() <= t.getTime());
    }
    return [...list]
      .sort((a, b) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime())
      .map((e) => ({
        label: e.nombre + ' · ' + new Date(e.fecha).toLocaleDateString(),
        value: e.id
      }));
  });

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
        const opc = this.opciones();
        const validoLabel = opc.find((o) => o.value === desdeQuery);
        const pick = validoLabel ? desdeQuery : opc.length > 0 ? opc[0].value : null;
        if (pick != null) {
          this.cambiarEvento(pick);
        }
      },
      error: () => this.cargando.set(false)
    });
  }

  alCambiarFiltrosLista() {
    const id = this.eventoSeleccionado();
    const opc = this.opciones();
    const aun = id != null && opc.some((o) => o.value === id);
    if (aun) return;
    this.eventoSeleccionado.set(null);
    this.reporte.set(null);
    const primera = opc[0]?.value;
    if (primera != null) this.cambiarEvento(primera);
  }

  cambiarEvento(id: number | null | undefined) {
    if (id == null) return;
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

  descargarExcel() {
    const r = this.reporte();
    if (!r) return;
    this.exportSvc.exportarReporteAvanzadoExcel(`reporte_evento_${r.eventoId}`, r, 'organizador');
    this.messages.add({ severity: 'success', summary: 'Excel descargado' });
  }

  descargarPdf() {
    const r = this.reporte();
    if (!r) return;
    this.exportSvc.exportarReporteAvanzadoPdf(`reporte_evento_${r.eventoId}`, r, 'organizador');
    this.messages.add({ severity: 'success', summary: 'PDF descargado' });
  }

  abrirVistaPrevia() {
    if (this.reporte()) this.vistaPreviaVisible.set(true);
  }

  porcentajeAsistenciaSalida(r: ReporteEventoAvanzado): number {
    return r.checkInsTotal === 0 ? 0 : Math.round((r.checkOutsTotal / r.checkInsTotal) * 100);
  }

  formatearHora(h: number): string {
    const hora = ((h % 24) + 24) % 24;
    return hora.toString().padStart(2, '0') + ':00';
  }

  funcionLabel(f: string): string {
    switch (f) {
      case 'CHECK_IN_QR':
        return 'Check-in / QR';
      case 'CHECK_IN_MANUAL':
        return 'Check-in Manual';
      case 'REGISTRO_SALIDA':
        return 'Registro Salida';
      case 'GENERAL':
        return 'General';
      default:
        return f;
    }
  }
}
