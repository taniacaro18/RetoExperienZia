import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { merge, of, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, finalize, map, switchMap } from 'rxjs/operators';
import { AuthStore } from '../../core/auth/auth.store';
import { EventoApi } from '../../core/api/evento.api';
import { InscripcionApi } from '../../core/api/inscripcion.api';
import {
  AforoEnVivo,
  AsistenteEvento,
  EstadoInscripcion,
  Evento,
  EventoStaff,
  FuncionStaff,
  Inscripcion
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
export class StaffEventoDetallePage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly store = inject(AuthStore);
  private readonly eventoApi = inject(EventoApi);
  private readonly inscripcionApi = inject(InscripcionApi);
  private readonly messages = inject(MessageService);
  private readonly confirm = inject(ConfirmationService);

  /** Emite cada cambio de texto (se debouncea antes de llamar al API). */
  private readonly busquedaTiempoReal = new Subject<string>();
  /** Recarga la lista al instante (misma consulta o tras check-in / botón Buscar). */
  private readonly listaForzada = new Subject<string>();

  readonly cargando = signal(true);
  readonly buscandoLista = signal(false);
  readonly eventoId = signal<number | null>(null);
  readonly evento = signal<Evento | null>(null);
  readonly miAsignacion = signal<EventoStaff | null>(null);
  readonly aforo = signal<AforoEnVivo | null>(null);
  readonly asistentes = signal<AsistenteEvento[]>([]);
  readonly busqueda = signal('');
  readonly filtro = signal<FiltroEstado>('TODOS');
  readonly procesando = signal<number | null>(null);
  readonly asistenteSeleccionadoId = signal<number | null>(null);

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

  readonly asistenteSeleccionado = computed(() => {
    const id = this.asistenteSeleccionadoId();
    if (id == null) return null;
    return this.asistentesFiltrados().find((x) => x.inscripcionId === id) ?? null;
  });

  readonly puedeCheckIn = computed(() => {
    const f = this.miAsignacion()?.funcion;
    return f === 'CHECK_IN_QR' || f === 'CHECK_IN_MANUAL' || f === 'GENERAL';
  });

  readonly puedeCheckOut = computed(() => {
    const f = this.miAsignacion()?.funcion;
    return f === 'REGISTRO_SALIDA' || f === 'GENERAL';
  });

  readonly checkInOutBloqueado = computed(() => {
    const e = this.evento()?.estado;
    return e === 'FINALIZADO' || e === 'CANCELADO';
  });

  estadoSeverity = inscripcionEstadoSeverity;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.cargando.set(false);
      return;
    }
    this.eventoId.set(id);
    this.cargando.set(true);

    merge(
      this.busquedaTiempoReal.pipe(
        debounceTime(350),
        map((q) => q.trim()),
        distinctUntilChanged()
      ),
      this.listaForzada.pipe(map((q) => q.trim()))
    )
      .pipe(
        switchMap((q) => this.fetchListaAsistentes(q)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (lista) => {
          this.asistentes.set(lista);
        },
        error: () => {
          this.buscandoLista.set(false);
        }
      });

    this.cargarMetadatosEvento();
    this.recargarAforo();
    this.listaForzada.next(this.busqueda().trim());
  }

  private cargarMetadatosEvento() {
    const staffId = this.store.usuario()?.id;
    const eventoId = this.eventoId();
    if (!staffId || !eventoId) return;

    this.eventoApi.obtener(eventoId).subscribe({
      next: (e) => {
        this.evento.set(e);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
      }
    });

    this.inscripcionApi.eventosDelStaff(staffId).subscribe({
      next: (asigs) => {
        const mia = asigs.find((a) => a.eventoId === eventoId) ?? null;
        this.miAsignacion.set(mia);
      },
      error: () => {}
    });
  }

  private fetchListaAsistentes(q: string) {
    const staffId = this.store.usuario()?.id;
    const eventoId = this.eventoId();
    if (!staffId || !eventoId) {
      this.buscandoLista.set(false);
      return of([]);
    }
    this.buscandoLista.set(true);
    const param = q.length > 0 ? q : undefined;
    return this.inscripcionApi.asistentesParaStaff(eventoId, staffId, param).pipe(
      finalize(() => this.buscandoLista.set(false))
    );
  }

  private recargarAforo() {
    const eventoId = this.eventoId();
    if (!eventoId) return;
    this.inscripcionApi.aforoEnVivo(eventoId).subscribe({
      next: (a) => this.aforo.set(a),
      error: () => {}
    });
  }

  onCambioBusqueda(valor: string) {
    this.busqueda.set(valor);
    this.asistenteSeleccionadoId.set(null);
    this.busquedaTiempoReal.next(valor);
  }

  cambiarFiltro(f: FiltroEstado) {
    this.filtro.set(f);
    this.asistenteSeleccionadoId.set(null);
  }

  refrescar() {
    this.listaForzada.next(this.busqueda().trim());
    this.recargarAforo();
  }

  buscarAsistentes() {
    this.listaForzada.next(this.busqueda().trim());
  }

  seleccionarFila(a: AsistenteEvento, ev: MouseEvent) {
    const el = ev.target as HTMLElement;
    if (el.closest('button')) {
      return;
    }
    this.asistenteSeleccionadoId.update((id) => (id === a.inscripcionId ? null : a.inscripcionId));
  }

  filaSeleccionada(a: AsistenteEvento): boolean {
    return this.asistenteSeleccionadoId() === a.inscripcionId;
  }

  etiquetaEstadoInscripcion(estado: EstadoInscripcion): string {
    if (estado === 'ASISTIO') return 'INGRESADO';
    if (estado === 'INSCRITO') return 'PENDIENTE DE INGRESO';
    return estado;
  }

  marcarCheckIn(a: AsistenteEvento) {
    const staffId = this.store.usuario()?.id;
    if (!staffId) return;
    if (a.estadoInscripcion !== 'INSCRITO') {
      this.messages.add({
        severity: 'info',
        summary: 'Ya ingresado',
        detail: 'Este asistente ya figura con ingreso registrado en el evento.',
        life: 4500
      });
      return;
    }
    if (!this.puedeCheckIn()) {
      this.messages.add({
        severity: 'warn',
        summary: 'Acción no autorizada',
        detail: 'Tu función no permite hacer check-in.'
      });
      return;
    }
    if (this.checkInOutBloqueado()) {
      this.messages.add({
        severity: 'warn',
        summary: 'Evento cerrado',
        detail: 'Este evento ya no admite registrar asistencia.'
      });
      return;
    }
    this.confirm.confirm({
      header: 'Confirmar check-in',
      message: this.mensajeConfirmCheckIn(a),
      icon: 'pi pi-sign-in',
      acceptLabel: 'Registrar entrada',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: '!bg-emerald-600 !border-emerald-600',
      accept: () => {
        this.procesando.set(a.inscripcionId);
        this.inscripcionApi.checkIn(a.inscripcionId, staffId).subscribe({
          next: (ins) => {
            this.procesando.set(null);
            this.messages.add({
              severity: 'success',
              summary: 'Check-in registrado',
              detail: this.detalleToastCheckInOut(ins, a.nombre),
              life: 5500
            });
            this.actualizarAsistente(a.inscripcionId, ins.estado, ins.fechaCheckIn, ins.fechaCheckOut);
            this.recargarAforo();
            this.listaForzada.next(this.busqueda().trim());
          },
          error: (err) => {
            this.procesando.set(null);
            this.mostrarErrorCheckInOut(err, 'entrada');
          }
        });
      }
    });
  }

  private mensajeConfirmCheckIn(a: AsistenteEvento): string {
    const ev = this.evento();
    const doc =
      a.numeroDocumento != null && String(a.numeroDocumento).trim().length > 0
        ? `${a.tipoDocumento ?? ''} ${a.numeroDocumento}`.trim()
        : '—';
    const lineas = [`Asistente: ${a.nombre}`, `Documento: ${doc}`];
    if (ev) {
      lineas.push(`Evento: ${ev.nombre}`);
      lineas.push(`Fecha: ${new Date(ev.fecha).toLocaleString()}`);
      if (ev.ubicacion?.trim()) {
        lineas.push(`Ubicación: ${ev.ubicacion.trim()}`);
      }
    }
    lineas.push('', '¿Confirmas el registro de entrada?');
    return lineas.join('\n');
  }

  private detalleToastCheckInOut(ins: Inscripcion, fallbackNombre: string): string {
    const nombre = ins.nombreAsistente?.trim() || fallbackNombre;
    const doc =
      ins.tipoDocumento && ins.numeroDocumento
        ? `${ins.tipoDocumento} ${ins.numeroDocumento}`.trim()
        : ins.numeroDocumento?.trim() || '';
    const ev = ins.nombreEvento?.trim();
    const fechaEv = ins.fechaEvento ? new Date(ins.fechaEvento).toLocaleString() : '';
    const ub = ins.ubicacionEvento?.trim();
    return [nombre, doc ? `Documento: ${doc}` : null, ev ? `Evento: ${ev}` : null, fechaEv ? fechaEv : null, ub ? `Ubicación: ${ub}` : null]
      .filter(Boolean)
      .join(' · ');
  }

  private mensajeConfirmCheckOut(a: AsistenteEvento): string {
    const ev = this.evento();
    const doc =
      a.numeroDocumento != null && String(a.numeroDocumento).trim().length > 0
        ? `${a.tipoDocumento ?? ''} ${a.numeroDocumento}`.trim()
        : '—';
    const lineas = [`Asistente: ${a.nombre}`, `Documento: ${doc}`];
    if (ev) {
      lineas.push(`Evento: ${ev.nombre}`);
    }
    lineas.push('', '¿Confirmas el registro de salida?');
    return lineas.join('\n');
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
    if (this.checkInOutBloqueado()) {
      this.messages.add({
        severity: 'warn',
        summary: 'Evento cerrado',
        detail: 'Este evento ya no admite registrar salidas.'
      });
      return;
    }
    this.confirm.confirm({
      header: 'Registrar salida',
      message: this.mensajeConfirmCheckOut(a),
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
              detail: this.detalleToastCheckInOut(ins, a.nombre),
              life: 5500
            });
            this.actualizarAsistente(a.inscripcionId, ins.estado, ins.fechaCheckIn, ins.fechaCheckOut);
            this.recargarAforo();
            this.listaForzada.next(this.busqueda().trim());
          },
          error: (err) => {
            this.procesando.set(null);
            this.mostrarErrorCheckInOut(err, 'salida');
          }
        });
      }
    });
  }

  private mostrarErrorCheckInOut(err: unknown, tipo: 'entrada' | 'salida') {
    const http = err as HttpErrorResponse;
    const raw = this.extraerMensajeHttp(http);
    const lower = raw.toLowerCase();

    if (http.status === 409 || lower.includes('aforo')) {
      this.messages.add({
        severity: 'warn',
        summary: 'Aforo completo',
        detail:
          'El evento alcanzó el límite de aforo permitido. No se puede registrar otro ingreso en este momento.',
        life: 6000
      });
      return;
    }

    if (
      lower.includes('ya fue utilizado') ||
      lower.includes('primer ingreso') ||
      lower.includes('ya tiene salida') ||
      lower.includes('ya se marcó')
    ) {
      this.messages.add({
        severity: 'warn',
        summary: tipo === 'entrada' ? 'Ingreso duplicado' : 'Salida no válida',
        detail:
          tipo === 'entrada'
            ? 'Este asistente ya registró su ingreso. El código QR ya no es válido para un nuevo ingreso.'
            : raw,
        life: 6500
      });
      this.listaForzada.next(this.busqueda().trim());
      return;
    }

    this.messages.add({
      severity: 'warn',
      summary: tipo === 'entrada' ? 'No se pudo registrar el ingreso' : 'No se pudo registrar la salida',
      detail: raw,
      life: 5500
    });
  }

  private extraerMensajeHttp(http: HttpErrorResponse): string {
    const body = http?.error;
    if (body && typeof body === 'object' && 'message' in body) {
      return String((body as { message: unknown }).message);
    }
    if (typeof body === 'string' && body.trim().length > 0) {
      return body;
    }
    return http?.message || 'Error al contactar el servidor.';
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
      case 'CHECK_IN_QR':
        return 'Check-in / QR';
      case 'CHECK_IN_MANUAL':
        return 'Check-in Manual';
      case 'REGISTRO_SALIDA':
        return 'Registro Salida';
      case 'GENERAL':
        return 'General';
      default:
        return '—';
    }
  }
}
