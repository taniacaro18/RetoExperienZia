import { Component, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TagModule } from 'primeng/tag';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { EventoApi } from '../../core/api/evento.api';
import { Evento } from '../../core/models/domain.models';

@Component({
  selector: 'app-detalle-evento-publico-page',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterLink, TagModule, ProgressSpinnerModule],
  templateUrl: './detalle-evento-publico.page.html',
  styleUrl: './detalle-evento-publico.page.scss'
})
export class DetalleEventoPublicoPage {
  private readonly route = inject(ActivatedRoute);
  private readonly eventoApi = inject(EventoApi);

  readonly cargando = signal(true);
  readonly evento = signal<Evento | null>(null);
  readonly error = signal<string | null>(null);

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error.set('ID de evento no válido.');
      this.cargando.set(false);
      return;
    }
    this.eventoApi.obtenerPublico(id).subscribe({
      next: (e) => {
        this.evento.set(e);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se encontró el evento o no está disponible.');
        this.cargando.set(false);
      }
    });
  }

  cupos(): number {
    const e = this.evento();
    return e ? Math.max(0, e.aforoMaximo - e.aforoActual) : 0;
  }

  porcentajeAforo(): number {
    const e = this.evento();
    if (!e || e.aforoMaximo === 0) return 0;
    return Math.round((e.aforoActual / e.aforoMaximo) * 100);
  }
}
