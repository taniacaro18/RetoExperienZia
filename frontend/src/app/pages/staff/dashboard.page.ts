import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { AuthStore } from '../../core/auth/auth.store';
import { InscripcionApi } from '../../core/api/inscripcion.api';
import { EventoStaff } from '../../core/models/domain.models';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';
import { AforoBarComponent } from '../../shared/aforo-bar/aforo-bar.component';
import { eventoEstadoLabel, eventoEstadoSeverity } from '../../shared/estado.helpers';

@Component({
  selector: 'app-staff-dashboard-page',
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
export class StaffDashboardPage {
  private readonly store = inject(AuthStore);
  private readonly api = inject(InscripcionApi);

  readonly cargando = signal(true);
  readonly eventos = signal<EventoStaff[]>([]);

  readonly hoy = computed(() => {
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const finHoy = new Date(hoy);
    finHoy.setHours(23, 59, 59, 999);
    return this.eventos().filter((e) => {
      const f = new Date(e.fechaEvento);
      return f >= hoy && f <= finHoy;
    });
  });

  readonly proximos = computed(() => {
    const ahora = new Date();
    return this.eventos()
      .filter((e) => new Date(e.fechaEvento) >= ahora && e.estadoEvento === 'ACTIVO')
      .sort((a, b) => new Date(a.fechaEvento).getTime() - new Date(b.fechaEvento).getTime())
      .slice(0, 6);
  });

  readonly stats = computed(() => {
    const items = this.eventos();
    return {
      total: items.length,
      activos: items.filter((e) => e.estadoEvento === 'ACTIVO').length,
      hoy: this.hoy().length,
      finalizados: items.filter((e) => e.estadoEvento === 'FINALIZADO').length
    };
  });

  estadoLabel = eventoEstadoLabel;
  estadoSeverity = eventoEstadoSeverity;

  ngOnInit() {
    const id = this.store.usuario()?.id;
    if (!id) return;
    this.cargando.set(true);
    this.api.eventosDelStaff(id).subscribe({
      next: (lista) => {
        this.eventos.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
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
}
