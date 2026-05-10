import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { AuthStore } from '../../core/auth/auth.store';
import { InscripcionApi } from '../../core/api/inscripcion.api';
import { EventoApi } from '../../core/api/evento.api';
import { CertificadoApi } from '../../core/api/certificado.api';
import { Certificado, Evento, Inscripcion } from '../../core/models/domain.models';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';

/**
 * Página de inicio (Home) del usuario autenticado.
 * - Si es ORGANIZADOR / STAFF / ADMIN, redirige a su dashboard.
 * - Si es ASISTENTE muestra su mini-dashboard con:
 *   próximo evento, KPIs (inscripciones activas / eventos asistidos / certificados)
 *   y accesos rápidos al catálogo, mis inscripciones y mis certificados.
 */
@Component({
  selector: 'app-inicio-page',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    ProgressSpinnerModule,
    StatCardComponent
  ],
  templateUrl: './inicio.page.html'
})
export class InicioPage {
  readonly store = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly inscripcionApi = inject(InscripcionApi);
  private readonly eventoApi = inject(EventoApi);
  private readonly certificadoApi = inject(CertificadoApi);

  readonly cargando = signal<boolean>(true);
  readonly inscripciones = signal<Inscripcion[]>([]);
  readonly eventos = signal<Evento[]>([]);
  readonly certificados = signal<Certificado[]>([]);

  /** Inscripciones activas (no canceladas) ordenadas por fecha de evento ascendente. */
  readonly inscripcionesActivas = computed(() => {
    const eventosMap = new Map(this.eventos().map((e) => [e.id, e]));
    return this.inscripciones()
      .filter((i) => i.estado !== 'CANCELADO')
      .map((i) => ({ inscripcion: i, evento: eventosMap.get(i.eventoId) }))
      .filter((x): x is { inscripcion: Inscripcion; evento: Evento } => !!x.evento)
      .sort((a, b) => new Date(a.evento.fecha).getTime() - new Date(b.evento.fecha).getTime());
  });

  /** Cantidad de inscripciones futuras (eventos cuya fecha aún no pasó). */
  readonly proximasCount = computed(() => {
    const ahora = Date.now();
    return this.inscripcionesActivas().filter(
      (x) => new Date(x.evento.fecha).getTime() >= ahora
    ).length;
  });

  /** Próximo evento inscrito al que el usuario asistirá (el más cercano en el futuro). */
  readonly proximoEvento = computed(() => {
    const ahora = Date.now();
    return this.inscripcionesActivas().find(
      (x) => new Date(x.evento.fecha).getTime() >= ahora
    );
  });

  readonly asistidosCount = computed(
    () => this.inscripciones().filter((i) => i.estado === 'ASISTIO').length
  );

  readonly certificadosCount = computed(() => this.certificados().length);

  /** Eventos públicos sugeridos: activos, futuros, en los que aún no estoy inscrito. */
  readonly sugeridos = computed(() => {
    const idsInscritos = new Set(this.inscripciones().map((i) => i.eventoId));
    const ahora = Date.now();
    return this.eventos()
      .filter(
        (e) =>
          e.estado === 'ACTIVO' &&
          e.tipoEvento === 'PUBLICO' &&
          new Date(e.fecha).getTime() >= ahora &&
          !idsInscritos.has(e.id)
      )
      .sort((a, b) => new Date(a.fecha).getTime() - new Date(b.fecha).getTime())
      .slice(0, 3);
  });

  ngOnInit() {
    const rol = this.store.rol();
    if (rol === 'STAFF') {
      this.router.navigate(['/staff/dashboard'], { replaceUrl: true });
      return;
    }
    if (rol === 'ORGANIZADOR') {
      this.router.navigate(['/organizador/dashboard'], { replaceUrl: true });
      return;
    }
    if (rol === 'ADMIN') {
      this.router.navigate(['/admin/dashboard'], { replaceUrl: true });
      return;
    }

    const userId = this.store.usuario()?.id;
    if (!userId) {
      this.cargando.set(false);
      return;
    }

    forkJoin({
      inscripciones: this.inscripcionApi.listarPorUsuario(userId),
      eventos: this.eventoApi.catalogoPublicos(),
      certificados: this.certificadoApi.listarPorUsuario(userId)
    }).subscribe({
      next: ({ inscripciones, eventos, certificados }) => {
        this.inscripciones.set(inscripciones);
        this.eventos.set(eventos);
        this.certificados.set(certificados);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
  }

  primeraLetra(nombre?: string | null): string {
    if (!nombre) return '?';
    return nombre.trim().charAt(0).toUpperCase();
  }
}
