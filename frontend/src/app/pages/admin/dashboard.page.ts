import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { ReporteApi } from '../../core/api/reporte.api';
import { EventoApi } from '../../core/api/evento.api';
import { UsuarioApi } from '../../core/api/usuario.api';
import { PagoApi } from '../../core/api/pago.api';
import { DashboardAdmin, Evento, Pago, Usuario } from '../../core/models/domain.models';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';
import {
  eventoEstadoLabel,
  eventoEstadoSeverity,
  rolLabel,
  rolSeverity,
  usuarioEstadoSeverity
} from '../../shared/estado.helpers';

@Component({
  selector: 'app-admin-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    ProgressSpinnerModule,
    TagModule,
    StatCardComponent
  ],
  templateUrl: './dashboard.page.html'
})
export class AdminDashboardPage {
  private readonly reporteApi = inject(ReporteApi);
  private readonly eventoApi = inject(EventoApi);
  private readonly usuarioApi = inject(UsuarioApi);
  private readonly pagoApi = inject(PagoApi);

  readonly cargando = signal(true);
  readonly stats = signal<DashboardAdmin | null>(null);
  readonly eventosPendientes = signal<Evento[]>([]);
  readonly organizadoresPendientes = signal<Usuario[]>([]);
  readonly pagosPendientes = signal<Pago[]>([]);

  readonly maxEventoSerie = computed(() => {
    const items = this.stats()?.serieMensualEventos ?? [];
    const m = Math.max(0, ...items.map((p) => p.valor));
    return m === 0 ? 1 : m;
  });
  readonly maxUsuarioSerie = computed(() => {
    const items = this.stats()?.serieMensualUsuarios ?? [];
    const m = Math.max(0, ...items.map((p) => p.valor));
    return m === 0 ? 1 : m;
  });

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
        const pend = lista
          .filter((e) => e.estado === 'PENDIENTE')
          .sort((a, b) => new Date(a.fecha).getTime() - new Date(b.fecha).getTime())
          .slice(0, 5);
        this.eventosPendientes.set(pend);
      }
    });

    this.usuarioApi.buscar({ rol: 'ORGANIZADOR', estado: 'PENDIENTE' }).subscribe({
      next: (lista) => this.organizadoresPendientes.set(lista.slice(0, 5))
    });

    this.pagoApi.listarPendientes().subscribe({
      next: (lista) => this.pagosPendientes.set(lista.slice(0, 5))
    });
  }
}
