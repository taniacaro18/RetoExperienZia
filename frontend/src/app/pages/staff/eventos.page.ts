import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { AuthStore } from '../../core/auth/auth.store';
import { InscripcionApi } from '../../core/api/inscripcion.api';
import { EventoStaff } from '../../core/models/domain.models';
import { AforoBarComponent } from '../../shared/aforo-bar/aforo-bar.component';
import { eventoEstadoLabel, eventoEstadoSeverity } from '../../shared/estado.helpers';

type FiltroEstado = 'TODOS' | 'PROXIMOS' | 'HOY' | 'PASADOS';

@Component({
  selector: 'app-staff-eventos-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    RouterLink,
    ProgressSpinnerModule,
    TagModule,
    InputTextModule,
    AforoBarComponent
  ],
  templateUrl: './eventos.page.html'
})
export class StaffEventosPage {
  private readonly store = inject(AuthStore);
  private readonly api = inject(InscripcionApi);

  readonly cargando = signal(true);
  readonly eventos = signal<EventoStaff[]>([]);
  readonly busqueda = signal('');
  readonly filtro = signal<FiltroEstado>('TODOS');

  readonly eventosFiltrados = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    const f = this.filtro();
    let lista = [...this.eventos()];
    const ahora = new Date();
    const hoy = new Date(); hoy.setHours(0, 0, 0, 0);
    const finHoy = new Date(); finHoy.setHours(23, 59, 59, 999);

    if (f === 'PROXIMOS') {
      lista = lista.filter((e) => new Date(e.fechaEvento) >= ahora);
    } else if (f === 'HOY') {
      lista = lista.filter((e) => {
        const fe = new Date(e.fechaEvento);
        return fe >= hoy && fe <= finHoy;
      });
    } else if (f === 'PASADOS') {
      lista = lista.filter((e) => new Date(e.fechaEvento) < ahora);
    }

    if (q) {
      lista = lista.filter(
        (e) =>
          e.nombreEvento.toLowerCase().includes(q) ||
          (e.categoria || '').toLowerCase().includes(q) ||
          (e.ubicacion || '').toLowerCase().includes(q)
      );
    }
    lista.sort((a, b) => new Date(a.fechaEvento).getTime() - new Date(b.fechaEvento).getTime());
    return lista;
  });

  readonly conteo = computed(() => {
    const ahora = new Date();
    const hoy = new Date(); hoy.setHours(0, 0, 0, 0);
    const finHoy = new Date(); finHoy.setHours(23, 59, 59, 999);
    const items = this.eventos();
    return {
      total: items.length,
      proximos: items.filter((e) => new Date(e.fechaEvento) >= ahora).length,
      hoy: items.filter((e) => {
        const f = new Date(e.fechaEvento);
        return f >= hoy && f <= finHoy;
      }).length,
      pasados: items.filter((e) => new Date(e.fechaEvento) < ahora).length
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

  esHoy(e: EventoStaff): boolean {
    const f = new Date(e.fechaEvento);
    const hoy = new Date(); hoy.setHours(0, 0, 0, 0);
    const finHoy = new Date(); finHoy.setHours(23, 59, 59, 999);
    return f >= hoy && f <= finHoy;
  }
}
