import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { SelectModule } from 'primeng/select';
import { MessageService } from 'primeng/api';
import { ReporteApi, EventoPopular, ReporteEventoAvanzado } from '../../core/api/reporte.api';
import { EventoApi } from '../../core/api/evento.api';
import { DashboardAdmin, Evento } from '../../core/models/domain.models';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';
import { DonutComponent } from '../../shared/donut/donut.component';
import { ExportService } from '../../core/export/export.service';

@Component({
  selector: 'app-admin-reportes-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    RouterLink,
    ProgressSpinnerModule,
    TagModule,
    SelectModule,
    StatCardComponent,
    DonutComponent
  ],
  templateUrl: './reportes.page.html'
})
export class AdminReportesPage {
  private readonly reporteApi = inject(ReporteApi);
  private readonly eventoApi = inject(EventoApi);
  private readonly messages = inject(MessageService);
  private readonly exportSvc = inject(ExportService);

  readonly cargando = signal(true);
  readonly stats = signal<DashboardAdmin | null>(null);
  readonly populares = signal<EventoPopular[]>([]);
  readonly eventos = signal<Evento[]>([]);
  readonly eventoSeleccionado = signal<number | null>(null);
  readonly cargandoAvanzado = signal(false);
  readonly reporteAvanzado = signal<ReporteEventoAvanzado | null>(null);

  readonly opcionesEventos = computed(() =>
    this.eventos()
      .filter((e) => e.estado === 'ACTIVO' || e.estado === 'FINALIZADO')
      .sort((a, b) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime())
      .map((e) => ({
        label: e.nombre + ' · ' + new Date(e.fecha).toLocaleDateString(),
        value: e.id
      }))
  );

  readonly maxPopulares = computed(() => {
    const m = Math.max(0, ...this.populares().map((p) => p.totalInscritos));
    return m === 0 ? 1 : m;
  });

  readonly segmentosCheckIn = computed(() => {
    const r = this.reporteAvanzado();
    if (!r) return [];
    return [
      { label: 'Por QR', valor: r.checkInsPorQR, color: '#7C3AED' },
      { label: 'Manuales', valor: r.checkInsManuales, color: '#10B981' }
    ];
  });

  readonly segmentosAsistencia = computed(() => {
    const r = this.reporteAvanzado();
    if (!r) return [];
    return [
      { label: 'Asistieron', valor: r.asistieron, color: '#10B981' },
      { label: 'Faltaron', valor: r.faltaron, color: '#F87171' }
    ];
  });

  readonly maxCurva = computed(() => {
    const r = this.reporteAvanzado();
    if (!r) return 1;
    const m = Math.max(0, ...r.curvaIngreso.map((c) => Math.max(c.ingresos, c.salidas)));
    return m === 0 ? 1 : m;
  });

  ngOnInit() {
    this.cargando.set(true);
    this.reporteApi.dashboardAdmin().subscribe({
      next: (s) => {
        this.stats.set(s);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
    this.reporteApi.eventosPopulares().subscribe({
      next: (lista) => this.populares.set(lista.slice(0, 10))
    });
    this.eventoApi.listar().subscribe({
      next: (lista) => this.eventos.set(lista)
    });
  }

  cargarAvanzado() {
    const id = this.eventoSeleccionado();
    if (!id) return;
    this.cargandoAvanzado.set(true);
    this.reporteAvanzado.set(null);
    this.reporteApi.reporteAvanzado(id).subscribe({
      next: (r) => {
        this.reporteAvanzado.set(r);
        this.cargandoAvanzado.set(false);
      },
      error: () => {
        this.cargandoAvanzado.set(false);
        this.messages.add({
          severity: 'warn',
          summary: 'Sin datos',
          detail: 'No se pudo obtener el reporte avanzado para este evento.'
        });
      }
    });
  }

  private filasResumen(): { metrica: string; valor: number }[] {
    const s = this.stats();
    if (!s) return [];
    return [
      { metrica: 'Eventos totales', valor: s.eventosTotales },
      { metrica: 'Eventos activos', valor: s.eventosActivos },
      { metrica: 'Eventos pendientes', valor: s.eventosPendientes },
      { metrica: 'Eventos cancelados', valor: s.eventosCancelados },
      { metrica: 'Usuarios totales', valor: s.usuariosTotales },
      { metrica: 'Usuarios activos', valor: s.usuariosActivos },
      { metrica: 'Usuarios pendientes', valor: s.usuariosPendientes },
      { metrica: 'Organizadores activos', valor: s.organizadoresActivos },
      { metrica: 'Asistentes', valor: s.asistentesTotales },
      { metrica: 'Staff', valor: s.staffTotales },
      { metrica: 'Inscripciones totales', valor: s.inscripcionesTotales }
    ];
  }

  exportarResumenExcel() {
    const filas = this.filasResumen();
    if (filas.length === 0) return;
    this.exportSvc.exportarExcel('resumen_plataforma', 'Resumen', [
      { header: 'Métrica', value: (r) => r.metrica },
      { header: 'Valor', value: (r) => r.valor }
    ], filas);
  }

  exportarResumenPdf() {
    const filas = this.filasResumen();
    if (filas.length === 0) return;
    this.exportSvc.exportarPdf('resumen_plataforma',
      'Resumen general · ExperienZia', [
        { header: 'Métrica', value: (r) => r.metrica },
        { header: 'Valor', value: (r) => r.valor }
      ], filas);
  }

  exportarPopularesExcel() {
    const items = this.populares();
    if (!items.length) return;
    this.exportSvc.exportarExcel('eventos_populares', 'Populares', [
      { header: 'ID Evento', value: (p: EventoPopular) => p.eventoId },
      { header: 'Nombre', value: (p: EventoPopular) => p.nombre },
      { header: 'Inscritos', value: (p: EventoPopular) => p.totalInscritos }
    ], items);
  }

  exportarPopularesPdf() {
    const items = this.populares();
    if (!items.length) return;
    this.exportSvc.exportarPdf('eventos_populares',
      'Eventos más populares · ExperienZia', [
        { header: 'ID Evento', value: (p: EventoPopular) => p.eventoId },
        { header: 'Nombre', value: (p: EventoPopular) => p.nombre },
        { header: 'Inscritos', value: (p: EventoPopular) => p.totalInscritos }
      ], items);
  }
}
