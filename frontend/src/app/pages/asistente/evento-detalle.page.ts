import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { distinctUntilChanged, filter, map } from 'rxjs/operators';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { EventoApi } from '../../core/api/evento.api';
import { InscripcionApi } from '../../core/api/inscripcion.api';
import { Evento, Inscripcion } from '../../core/models/domain.models';
import { AuthStore } from '../../core/auth/auth.store';
import { eventoVentanaYaCerro } from '../../shared/evento-catalogo.helpers';
import { eventoEstadoLabel } from '../../shared/estado.helpers';
import { AforoBarComponent } from '../../shared/aforo-bar/aforo-bar.component';
import { destinoRetornoEventoDetalle } from '../../core/navigation/retorno-evento-detalle';

@Component({
  selector: 'app-evento-detalle-page',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    ButtonModule,
    ProgressSpinnerModule,
    ConfirmDialogModule,
    AforoBarComponent
  ],
  templateUrl: './evento-detalle.page.html',
  styleUrl: './evento-detalle.page.scss'
})
export class EventoDetallePage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly eventosApi = inject(EventoApi);
  private readonly inscripcionesApi = inject(InscripcionApi);
  private readonly auth = inject(AuthStore);
  private readonly messages = inject(MessageService);
  private readonly confirma = inject(ConfirmationService);

  readonly cargando = signal(true);
  readonly evento = signal<Evento | null>(null);
  readonly miInscripcion = signal<Inscripcion | null>(null);
  readonly procesando = signal(false);
  /** Clave `retorno` del query (solo valores admitidos en `destinoRetornoEventoDetalle`). */
  readonly retornoKey = signal<string | null>(null);

  readonly cuposDisponibles = computed(() => {
    const e = this.evento();
    return e ? Math.max(0, e.aforoMaximo - e.aforoActual) : 0;
  });

  readonly porcentajeOcupacion = computed(() => {
    const e = this.evento();
    if (!e?.aforoMaximo) return 0;
    return Math.min(100, Math.round((e.aforoActual / e.aforoMaximo) * 100));
  });

  /** Solo los asistentes pueden inscribirse: organizador (de su propio evento), admin y staff no. */
  readonly puedeInscribirse = computed(() => {
    const e = this.evento();
    const u = this.auth.usuario();
    if (!e || !u) return false;
    if (eventoVentanaYaCerro(e)) return false;
    if (u.rol !== 'ASISTENTE') return false;
    if (e.organizadorId === u.id) return false;
    return true;
  });

  /** El usuario actual es el organizador dueño de este evento. */
  readonly soyOrganizadorDelEvento = computed(() => {
    const e = this.evento();
    const u = this.auth.usuario();
    return !!(e && u && e.organizadorId === u.id);
  });

  readonly textoVolver = computed(() => {
    const porQuery = destinoRetornoEventoDetalle(this.retornoKey());
    if (porQuery) return porQuery.label;
    const r = this.auth.rol();
    if (r === 'ORGANIZADOR') return 'Volver a mis eventos';
    if (r === 'STAFF') return 'Volver a mis asignaciones';
    if (r === 'ADMIN') return 'Volver a administración de eventos';
    return 'Volver al catálogo';
  });

  /** Estado mostrado: si ya pasó la ventana y el API aún dice ACTIVO, se muestra como finalizado. */
  readonly estadoBadge = computed(() => {
    const e = this.evento();
    if (!e) return 'PENDIENTE';
    if (e.estado === 'ACTIVO' && eventoVentanaYaCerro(e)) return 'FINALIZADO';
    return e.estado;
  });

  /** Cupos libres y evento activo en ventana → chip «Disponible». */
  readonly mostrarChipDisponible = computed(() => {
    const e = this.evento();
    if (!e || e.estado !== 'ACTIVO') return false;
    if (eventoVentanaYaCerro(e)) return false;
    return this.cuposDisponibles() > 0;
  });

  readonly eventoEstadoLabelFn = eventoEstadoLabel;

  ngOnInit() {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((qp) => {
      const raw = qp.get('retorno');
      this.retornoKey.set(raw && destinoRetornoEventoDetalle(raw) ? raw : null);
    });

    this.route.paramMap
      .pipe(
        map((pm) => Number(pm.get('id'))),
        filter((id) => Number.isFinite(id) && id > 0),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((id) => this.cargarEvento(id));
  }

  private cargarEvento(id: number) {
    this.cargando.set(true);
    this.evento.set(null);
    this.miInscripcion.set(null);
    this.eventosApi.obtener(id).subscribe({
      next: (e) => {
        this.evento.set(e);
        this.cargando.set(false);
        this.cargarMiInscripcion(id);
      },
      error: () => {
        this.cargando.set(false);
        this.evento.set(null);
      }
    });
  }

  private cargarMiInscripcion(eventoId: number) {
    const u = this.auth.usuario();
    if (!u) return;
    this.inscripcionesApi.listarPorUsuario(u.id).subscribe({
      next: (lista) => {
        const i = lista.find(
          (x) => x.eventoId === eventoId && x.estado !== 'CANCELADO'
        );
        this.miInscripcion.set(i ?? null);
      }
    });
  }

  inscribirse() {
    const e = this.evento();
    const u = this.auth.usuario();
    if (!e || !u) return;
    this.confirma.confirm({
      message: '¿Confirmas tu inscripción al evento "' + e.nombre + '"?',
      header: 'Confirmar inscripción',
      icon: 'pi pi-question-circle',
      acceptLabel: 'Sí, inscribirme',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: '!bg-brand-600 !border-brand-600',
      accept: () => {
        this.procesando.set(true);
        this.inscripcionesApi.inscribir(u.id, e.id).subscribe({
          next: (ins) => {
            this.miInscripcion.set(ins);
            this.evento.update((ev) => ev ? { ...ev, aforoActual: ev.aforoActual + 1 } : ev);
            this.procesando.set(false);
            this.messages.add({
              severity: 'success',
              summary: 'Inscripción exitosa',
              detail: 'Te enviamos tu código QR a "Mis inscripciones".',
              life: 4500
            });
          },
          error: () => this.procesando.set(false)
        });
      }
    });
  }

  cancelarInscripcion() {
    const ins = this.miInscripcion();
    if (!ins) return;
    this.confirma.confirm({
      message: '¿Cancelar tu inscripción a este evento? Liberarás un cupo.',
      header: 'Cancelar inscripción',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Sí, cancelar',
      rejectLabel: 'No',
      acceptButtonStyleClass: '!bg-red-600 !border-red-600',
      accept: () => {
        this.procesando.set(true);
        this.inscripcionesApi.cancelarInscripcion(ins.id).subscribe({
          next: () => {
            this.miInscripcion.set(null);
            this.evento.update((ev) => ev ? { ...ev, aforoActual: Math.max(0, ev.aforoActual - 1) } : ev);
            this.procesando.set(false);
            this.messages.add({
              severity: 'success',
              summary: 'Inscripción cancelada',
              detail: 'Tu cupo fue liberado.'
            });
          },
          error: () => this.procesando.set(false)
        });
      }
    });
  }

  volver() {
    const porQuery = destinoRetornoEventoDetalle(this.retornoKey());
    if (porQuery) {
      void this.router.navigateByUrl(porQuery.path);
      return;
    }
    const r = this.auth.rol();
    if (r === 'ORGANIZADOR') {
      void this.router.navigate(['/organizador/eventos']);
    } else if (r === 'STAFF') {
      void this.router.navigate(['/staff/eventos']);
    } else if (r === 'ADMIN') {
      void this.router.navigate(['/admin/eventos']);
    } else {
      void this.router.navigate(['/eventos']);
    }
  }

  ventanaCerrada(): boolean {
    const e = this.evento();
    return e ? eventoVentanaYaCerro(e) : true;
  }

  inicialTitulo(e: Evento): string {
    const n = (e.nombre ?? '?').trim();
    return n ? n.charAt(0).toUpperCase() : '?';
  }
}
