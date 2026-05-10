import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { AuthStore } from '../../core/auth/auth.store';
import { ReporteApi } from '../../core/api/reporte.api';
import { EventoApi } from '../../core/api/evento.api';
import { DashboardOrganizador, Evento } from '../../core/models/domain.models';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';
import { AforoBarComponent } from '../../shared/aforo-bar/aforo-bar.component';
import {
  eventoEstadoLabel,
  eventoEstadoSeverity
} from '../../shared/estado.helpers';

interface BarraSerie {
  periodo: string;
  valor: number;
  pct: number;
}

@Component({
  selector: 'app-org-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    ProgressSpinnerModule,
    TagModule,
    StatCardComponent,
    AforoBarComponent
  ],
  templateUrl: './dashboard.page.html'
})
export class OrgDashboardPage {
  private readonly store = inject(AuthStore);
  private readonly reporteApi = inject(ReporteApi);
  private readonly eventoApi = inject(EventoApi);

  readonly cargando = signal(true);
  readonly stats = signal<DashboardOrganizador | null>(null);
  readonly eventos = signal<Evento[]>([]);

  readonly proximos = computed(() => {
    const ahora = new Date();
    return this.eventos()
      .filter((e) => e.estado === 'ACTIVO' && new Date(e.fecha) >= ahora)
      .sort((a, b) => new Date(a.fecha).getTime() - new Date(b.fecha).getTime())
      .slice(0, 6);
  });

  readonly serieInscripciones = computed<BarraSerie[]>(() => {
    const serie = this.stats()?.serieMensualInscripciones ?? [];
    if (serie.length === 0) return [];
    const max = Math.max(1, ...serie.map((p) => p.valor));
    return serie.map((p) => ({
      periodo: this.formateoMes(p.periodo),
      valor: p.valor,
      pct: Math.round((p.valor / max) * 100)
    }));
  });

  readonly serieEventos = computed<BarraSerie[]>(() => {
    const serie = this.stats()?.serieMensualEventos ?? [];
    if (serie.length === 0) return [];
    const max = Math.max(1, ...serie.map((p) => p.valor));
    return serie.map((p) => ({
      periodo: this.formateoMes(p.periodo),
      valor: p.valor,
      pct: Math.round((p.valor / max) * 100)
    }));
  });

  readonly necesitaAtencion = computed(() => {
    return this.eventos().filter(
      (e) => e.estado === 'PENDIENTE' || e.estado === 'RECHAZADO'
    );
  });

  estadoLabel = eventoEstadoLabel;
  estadoSeverity = eventoEstadoSeverity;

  ngOnInit() {
    const orgId = this.store.usuario()?.id;
    if (!orgId) return;
    this.cargando.set(true);
    this.reporteApi.dashboardOrganizador(orgId).subscribe({
      next: (s) => this.stats.set(s),
      error: () => {}
    });
    this.eventoApi.listarPorOrganizador(orgId).subscribe({
      next: (lista) => {
        this.eventos.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
  }

  private formateoMes(periodo: string): string {
    // periodo viene como "2026-05" o similar
    const partes = periodo.split('-');
    if (partes.length < 2) return periodo;
    const meses = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
    const idx = Number(partes[1]) - 1;
    return meses[idx] ?? periodo;
  }
}
