import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { SelectModule } from 'primeng/select';
import { ReporteApi } from '../../core/api/reporte.api';
import { EventoApi } from '../../core/api/evento.api';
import { UsuarioApi } from '../../core/api/usuario.api';
import { PagoApi } from '../../core/api/pago.api';
import {
  DashboardAdmin,
  EstadoEvento,
  Evento,
  Pago,
  Usuario
} from '../../core/models/domain.models';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';
import { DonutComponent } from '../../shared/donut/donut.component';
import {
  eventoEstadoLabel,
  eventoEstadoSeverity,
  rolLabel,
  rolSeverity,
  usuarioEstadoSeverity
} from '../../shared/estado.helpers';

/**
 * Pantalla: Panel principal (dashboard) del administrador.
 * Rol: ADMIN.
 * Resume métricas de la plataforma, colas de pendientes y gráficos de eventos con filtros.
 */
@Component({
  selector: 'app-admin-dashboard-page',
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
  templateUrl: './dashboard.page.html'
})
export class AdminDashboardPage {
  private readonly reporteApi = inject(ReporteApi);
  private readonly eventoApi = inject(EventoApi);
  private readonly usuarioApi = inject(UsuarioApi);
  private readonly pagoApi = inject(PagoApi);

  // true mientras cargan los datos del dashboard
  readonly cargando = signal(true);
  // números globales que vienen del API de reportes
  readonly stats = signal<DashboardAdmin | null>(null);
  // todos los eventos para filtrar en los gráficos de abajo
  readonly todosEventos = signal<Evento[]>([]);
  // organizadores activos (para el selector de filtro)
  readonly organizadoresActivos = signal<Usuario[]>([]);
  // eventos que aún necesitan revisión del admin
  readonly eventosPendientes = signal<Evento[]>([]);
  // cuentas de organizador esperando aprobación
  readonly organizadoresPendientes = signal<Usuario[]>([]);
  // pagos con comprobante pendiente de validar
  readonly pagosPendientes = signal<Pago[]>([]);

  /** Alcance filtros vista admin (solo afectan gráficos inferiores) */
  readonly filtroOrgId = signal<number | null>(null);
  readonly filtroTipoEvento = signal<'TODOS' | 'PUBLICO' | 'PRIVADO'>('TODOS');
  readonly fechaDesde = signal('');
  readonly fechaHasta = signal('');

  // escala máxima del gráfico de eventos por mes (evita división por cero)
  readonly maxEventoSerie = computed(() => {
    const items = this.stats()?.serieMensualEventos ?? [];
    const m = Math.max(0, ...items.map((p) => p.valor));
    return m === 0 ? 1 : m;
  });
  // escala máxima del gráfico de usuarios por mes
  readonly maxUsuarioSerie = computed(() => {
    const items = this.stats()?.serieMensualUsuarios ?? [];
    const m = Math.max(0, ...items.map((p) => p.valor));
    return m === 0 ? 1 : m;
  });

  // lista para el dropdown de filtro por organizador
  readonly opcionesOrganizadoresFiltro = computed(() => [
    { label: 'Todos los organizadores', value: null as number | null },
    ...this.organizadoresActivos().map((o) => ({
      label: `${o.nombre} (#${o.id})`,
      value: o.id
    }))
  ]);

  // eventos que cumplen org, tipo y rango de fechas elegidos en filtros
  readonly eventosFiltradosVista = computed(() => {
    let list = [...this.todosEventos()];
    const oid = this.filtroOrgId();
    if (oid != null) list = list.filter((e) => e.organizadorId === oid);
    const tp = this.filtroTipoEvento();
    if (tp !== 'TODOS') list = list.filter((e) => e.tipoEvento === tp);
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
    return list;
  });

  // cuenta eventos por mes según la lista ya filtrada
  readonly serieMesEventosFiltrados = computed(() => {
    const map = new Map<string, number>();
    for (const e of this.eventosFiltradosVista()) {
      const d = new Date(e.fecha);
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      map.set(key, (map.get(key) ?? 0) + 1);
    }
    return [...map.entries()]
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([periodo, valor]) => ({ periodo, valor }));
  });

  // tope del eje Y para la serie filtrada de eventos
  readonly maxSerieFiltrada = computed(() => {
    const s = this.serieMesEventosFiltrados();
    const m = Math.max(0, ...s.map((p) => p.valor));
    return m === 0 ? 1 : m;
  });

  // porciones del donut según estado de los eventos filtrados
  readonly segmentosEstadoFiltrados = computed(() => {
    const cols: Record<string, string> = {
      PENDIENTE: '#ca8a04',
      PENDIENTE_REVISION: '#2563eb',
      PENDIENTE_SUPLEMENTO: '#ea580c',
      PENDIENTE_CANCELACION: '#be185d',
      ACTIVO: '#7C49AE',
      APROBADO: '#0891b2',
      FINALIZADO: '#10B981',
      RECHAZADO: '#DC2626',
      CANCELADO: '#78716c'
    };
    const counts = new Map<EstadoEvento, number>();
    for (const e of this.eventosFiltradosVista()) {
      counts.set(e.estado, (counts.get(e.estado) ?? 0) + 1);
    }
    return [...counts.entries()].map(([estado, valor]) => ({
      label: this.estadoLabel(estado),
      valor,
      color: cols[estado] ?? '#a78bfa'
    }));
  });

  // true si hay algo pendiente en eventos, organizadores o pagos
  readonly tieneAtencion = computed(
    () =>
      this.eventosPendientes().length > 0 ||
      this.organizadoresPendientes().length > 0 ||
      this.pagosPendientes().length > 0
  );

  estadoLabel = eventoEstadoLabel;
  estadoSeveridad = eventoEstadoSeverity;
  usuarioEstadoSev = usuarioEstadoSeverity;
  rolLabel = rolLabel;
  rolSev = rolSeverity;

  // al entrar pedimos stats, eventos, organizadores y pagos pendientes
  ngOnInit() {
    this.cargando.set(true);

    this.reporteApi.dashboardAdmin().subscribe({
      next: (s) => {
        this.stats.set(s);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });

    this.eventoApi.listar().subscribe({
      next: (lista) => {
        this.todosEventos.set(lista);
        const pend = lista
          .filter((e) =>
            [
              'PENDIENTE',
              'PENDIENTE_REVISION',
              'PENDIENTE_SUPLEMENTO',
              'PENDIENTE_CANCELACION'
            ].includes(e.estado)
          )
          .sort((a, b) => new Date(a.fecha).getTime() - new Date(b.fecha).getTime())
          .slice(0, 8);
        this.eventosPendientes.set(pend);
      }
    });

    this.usuarioApi.buscar({ rol: 'ORGANIZADOR', estado: 'ACTIVO' }).subscribe({
      next: (orgs) => this.organizadoresActivos.set(orgs)
    });

    this.usuarioApi.buscar({ rol: 'ORGANIZADOR', estado: 'PENDIENTE' }).subscribe({
      next: (lista) => this.organizadoresPendientes.set(lista.slice(0, 5))
    });

    this.pagoApi.listarPendientes().subscribe({
      next: (lista) => this.pagosPendientes.set(lista.slice(0, 5))
    });
  }
}
