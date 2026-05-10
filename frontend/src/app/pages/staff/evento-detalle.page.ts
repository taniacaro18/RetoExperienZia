import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { AuthStore } from '../../core/auth/auth.store';
import { EventoApi } from '../../core/api/evento.api';
import { InscripcionApi } from '../../core/api/inscripcion.api';
import {
  AforoEnVivo,
  AsistenteEvento,
  EstadoInscripcion,
  Evento,
  EventoStaff,
  FuncionStaff
} from '../../core/models/domain.models';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';
import { AforoBarComponent } from '../../shared/aforo-bar/aforo-bar.component';
import { inscripcionEstadoSeverity } from '../../shared/estado.helpers';

type FiltroEstado = 'TODOS' | EstadoInscripcion | 'PRESENTES';

@Component({
  selector: 'app-staff-evento-detalle-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    RouterLink,
    ProgressSpinnerModule,
    TagModule,
    InputTextModule,
    ConfirmDialogModule,
    StatCardComponent,
    AforoBarComponent
  ],
  providers: [ConfirmationService],
  templateUrl: './evento-detalle.page.html'
})
export class StaffEventoDetallePage {
  private readonly route = inject(ActivatedRoute);
  private readonly store = inject(AuthStore);
  private readonly eventoApi = inject(EventoApi);
  private readonly inscripcionApi = inject(InscripcionApi);
  private readonly messages = inject(MessageService);
  private readonly confirm = inject(ConfirmationService);

  readonly cargando = signal(true);
  readonly eventoId = signal<number | null>(null);
  readonly evento = signal<Evento | null>(null);
  readonly miAsignacion = signal<EventoStaff | null>(null);
  readonly aforo = signal<AforoEnVivo | null>(null);
  readonly asistentes = signal<AsistenteEvento[]>([]);
  readonly busqueda = signal('');
  readonly filtro = signal<FiltroEstado>('TODOS');
  readonly procesando = signal<number | null>(null);

  readonly stats = computed(() => {
    const items = this.asistentes();
    return {
      total: items.length,
      asistieron: items.filter((a) => a.estadoInscripcion === 'ASISTIO').length,
      inscritos: items.filter((a) => a.estadoInscripcion === 'INSCRITO').length,
      presentes: items.filter((a) => a.estadoInscripcion === 'ASISTIO' && !a.fechaCheckOut).length
    };
  });

  readonly asistentesFiltrados = computed(() => {
    const f = this.filtro();
    let lista = this.asistentes();
    if (f === 'PRESENTES') {
      lista = lista.filter((a) => a.estadoInscripcion === 'ASISTIO' && !a.fechaCheckOut);
    } else if (f !== 'TODOS') {
      lista = lista.filter((a) => a.estadoInscripcion === f);
    }
    return lista;
  });

  readonly puedeCheckIn = computed(() => {
    const f = this.miAsignacion()?.funcion;
    return f === 'CHECK_IN_QR' || f === 'CHECK_IN_MANUAL' || f === 'GENERAL';
  });

  readonly puedeCheckOut = computed(() => {
    const f = this.miAsignacion()?.funcion;
    return f === 'REGISTRO_SALIDA' || f === 'GENERAL';
  });

  estadoSeverity = inscripcionEstadoSeverity;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) return;
    this.eventoId.set(id);
    this.cargarTodo();
  }

  private cargarTodo() {
    const staffId = this.store.usuario()?.id;
    const eventoId = this.eventoId();
    if (!staffId || !eventoId) return;
    this.cargando.set(true);

    this.eventoApi.obtener(eventoId).subscribe({
      next: (e) => this.evento.set(e),
      error: () => {}
    });

    this.inscripcionApi.eventosDelStaff(staffId).subscribe({
      next: (asigs) => {
        const mia = asigs.find((a) => a.eventoId === eventoId) ?? null;
        this.miAsignacion.set(mia);
      },
      error: () => {}
    });

    this.recargarAsistentes();
    this.recargarAforo();
  }

  private recargarAsistentes() {
    const staffId = this.store.usuario()?.id;
    const eventoId = this.eventoId();
    if (!staffId || !eventoId) return;
    this.cargando.set(true);
    this.inscripcionApi.asistentesParaStaff(eventoId, staffId, this.busqueda()).subscribe({
      next: (lista) => {
        this.asistentes.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
  }

  private recargarAforo() {
    const eventoId = this.eventoId();
    if (!eventoId) return;
    this.inscripcionApi.aforoEnVivo(eventoId).subscribe({
      next: (a) => this.aforo.set(a),
      error: () => {}
    });
  }

  refrescar() {
    this.recargarAsistentes();
    this.recargarAforo();
  }

  buscarAsistentes() {
    this.recargarAsistentes();
  }

  marcarCheckIn(a: AsistenteEvento) {
    const staffId = this.store.usuario()?.id;
    if (!staffId) return;
    if (!this.puedeCheckIn()) {
      this.messages.add({
        severity: 'warn',
        summary: 'Acción no autorizada',
        detail: 'Tu función no permite hacer check-in.'
      });
      return;
    }
    this.procesando.set(a.inscripcionId);
    this.inscripcionApi.checkIn(a.inscripcionId, staffId).subscribe({
      next: (ins) => {
        this.procesando.set(null);
        this.messages.add({
          severity: 'success',
          summary: 'Check-in registrado',
          detail: `${a.nombre} marcó su entrada.`,
          life: 3500
        });
        this.actualizarAsistente(a.inscripcionId, ins.estado, ins.fechaCheckIn, ins.fechaCheckOut);
        this.recargarAforo();
      },
      error: () => this.procesando.set(null)
    });
  }

  marcarCheckOut(a: AsistenteEvento) {
    const staffId = this.store.usuario()?.id;
    if (!staffId) return;
    if (!this.puedeCheckOut()) {
      this.messages.add({
        severity: 'warn',
        summary: 'Acción no autorizada',
        detail: 'Tu función no permite registrar salidas.'
      });
      return;
    }
    this.confirm.confirm({
      header: 'Registrar salida',
      message: `¿Confirmas el check-out de ${a.nombre}?`,
      icon: 'pi pi-sign-out',
      acceptLabel: 'Confirmar salida',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: '!bg-brand-600 !border-brand-600',
      accept: () => {
        this.procesando.set(a.inscripcionId);
        this.inscripcionApi.checkOut(a.inscripcionId, staffId).subscribe({
          next: (ins) => {
            this.procesando.set(null);
            this.messages.add({
              severity: 'success',
              summary: 'Check-out registrado',
              detail: `${a.nombre} marcó su salida.`,
              life: 3500
            });
            this.actualizarAsistente(a.inscripcionId, ins.estado, ins.fechaCheckIn, ins.fechaCheckOut);
            this.recargarAforo();
          },
          error: () => this.procesando.set(null)
        });
      }
    });
  }

  private actualizarAsistente(
    inscripcionId: number,
    estado: EstadoInscripcion,
    fechaCheckIn?: string | null,
    fechaCheckOut?: string | null
  ) {
    this.asistentes.update((items) =>
      items.map((it) =>
        it.inscripcionId === inscripcionId
          ? {
              ...it,
              estadoInscripcion: estado,
              fechaCheckIn: fechaCheckIn ?? it.fechaCheckIn,
              fechaCheckOut: fechaCheckOut ?? it.fechaCheckOut
            }
          : it
      )
    );
  }

  funcionLabel(f?: FuncionStaff): string {
    switch (f) {
      case 'CHECK_IN_QR': return 'Check-in / QR';
      case 'CHECK_IN_MANUAL': return 'Check-in Manual';
      case 'REGISTRO_SALIDA': return 'Registro Salida';
      case 'GENERAL': return 'General';
      default: return '—';
    }
  }
}
