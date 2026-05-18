import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { DialogModule } from 'primeng/dialog';
import { MessageService } from 'primeng/api';
import { AuthStore } from '../../core/auth/auth.store';
import { DomSanitizer } from '@angular/platform-browser';
import { EventoApi } from '../../core/api/evento.api';
import { PagoApi } from '../../core/api/pago.api';
import { UsuarioApi } from '../../core/api/usuario.api';
import {
  esComprobanteImagen,
  esComprobantePdf,
  urlComprobantePago,
  urlComprobanteSeguraPago
} from '../../core/utils/comprobante.util';
import { EstadoEvento, Evento, EventoNovedad, Pago, Usuario } from '../../core/models/domain.models';
import { SalonDisponibilidadDialogComponent } from '../../shared/salon-disponibilidad/salon-disponibilidad-dialog.component';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';
import { AforoBarComponent } from '../../shared/aforo-bar/aforo-bar.component';
import { eventoEstadoLabel, eventoEstadoSeverity } from '../../shared/estado.helpers';

type FiltroEstado = 'TODOS' | EstadoEvento;
type FiltroTipoEvento = 'TODOS' | 'PUBLICO' | 'PRIVADO';

@Component({
  selector: 'app-admin-eventos-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    CurrencyPipe,
    RouterLink,
    ProgressSpinnerModule,
    TagModule,
    InputTextModule,
    TextareaModule,
    DialogModule,
    StatCardComponent,
    AforoBarComponent,
    SalonDisponibilidadDialogComponent
  ],
  templateUrl: './eventos.page.html',
  styleUrl: './eventos.page.scss'
})
export class AdminEventosPage {
  private readonly route = inject(ActivatedRoute);
  private readonly store = inject(AuthStore);
  private readonly eventoApi = inject(EventoApi);
  private readonly pagoApi = inject(PagoApi);
  private readonly usuarioApi = inject(UsuarioApi);
  private readonly messages = inject(MessageService);
  private readonly sanitizer = inject(DomSanitizer);

  readonly cargando = signal(true);
  readonly eventos = signal<Evento[]>([]);
  readonly organizadores = signal<Map<number, Usuario>>(new Map());
  readonly busqueda = signal('');
  readonly filtroEstado = signal<FiltroEstado>('TODOS');
  readonly filtroTipoEvento = signal<FiltroTipoEvento>('TODOS');
  readonly fechaDesde = signal('');
  readonly fechaHasta = signal('');
  readonly procesando = signal<number | null>(null);
  readonly idFocus = signal<number | null>(null);

  readonly mostrarModalRechazo = signal(false);
  readonly eventoARechazar = signal<Evento | null>(null);
  /** Rechazo de alta/revisión vs rechazo de solicitud de cancelación. */
  readonly tipoRechazo = signal<'evento' | 'cancelacion'>('evento');
  motivoRechazo = '';

  readonly mostrarModalDetalle = signal(false);
  readonly eventoDetalle = signal<Evento | null>(null);
  readonly cargandoDetalleModal = signal(false);
  readonly novedades = signal<EventoNovedad[]>([]);
  readonly cargandoNovedades = signal(false);
  readonly pagoEvento = signal<Pago | null>(null);
  readonly cargandoPago = signal(false);
  readonly mostrarComprobantePago = signal(false);
  readonly mostrarDisponibilidadSalon = signal(false);
  /** Evento usado al abrir el calendario desde la lista o el detalle (null = vista general). */
  readonly eventoParaSalon = signal<Evento | null>(null);

  estadoLabel = eventoEstadoLabel;

  readonly salonContext = computed(() => {
    const ev = this.eventoParaSalon();
    return {
      ubicacion: ev?.ubicacion?.trim() || 'Salón principal',
      excluirId: ev?.id ?? null,
      fecha: ev ? this.fechaEventoParaSalon(ev) : null,
      inicio: ev ? this.horaInicioParaSalon(ev) : null,
      fin: ev ? this.horaFinParaSalon(ev) : null
    };
  });
  estadoSeverity = eventoEstadoSeverity;

  readonly conteo = computed(() => {
    const items = this.eventos();
    return {
      total: items.length,
      pendientes: items.filter((e) => e.estado === 'PENDIENTE').length,
      revision: items.filter((e) => e.estado === 'PENDIENTE_REVISION').length,
      suplemento: items.filter((e) => e.estado === 'PENDIENTE_SUPLEMENTO').length,
      pendCancel: items.filter((e) => e.estado === 'PENDIENTE_CANCELACION').length,
      aprobados: items.filter((e) => e.estado === 'APROBADO').length,
      activos: items.filter((e) => e.estado === 'ACTIVO').length,
      rechazados: items.filter((e) => e.estado === 'RECHAZADO').length,
      cancelados: items.filter((e) => e.estado === 'CANCELADO').length,
      finalizados: items.filter((e) => e.estado === 'FINALIZADO').length
    };
  });

  readonly eventosFiltrados = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    const f = this.filtroEstado();
    const ft = this.filtroTipoEvento();
    let lista = [...this.eventos()];
    if (f !== 'TODOS') lista = lista.filter((e) => e.estado === f);
    if (ft !== 'TODOS') lista = lista.filter((e) => e.tipoEvento === ft);
    const d1 = this.fechaDesde();
    const d2 = this.fechaHasta();
    if (d1) {
      const t = new Date(d1);
      t.setHours(0, 0, 0, 0);
      lista = lista.filter((e) => new Date(e.fecha).getTime() >= t.getTime());
    }
    if (d2) {
      const t = new Date(d2);
      t.setHours(23, 59, 59, 999);
      lista = lista.filter((e) => new Date(e.fecha).getTime() <= t.getTime());
    }
    if (q) {
      lista = lista.filter(
        (e) =>
          e.nombre.toLowerCase().includes(q) ||
          (e.categoria || '').toLowerCase().includes(q) ||
          this.nombreOrganizador(e.organizadorId).toLowerCase().includes(q)
      );
    }
    lista.sort((a, b) => {
      const orden = {
        PENDIENTE: 0,
        PENDIENTE_REVISION: 1,
        PENDIENTE_SUPLEMENTO: 2,
        PENDIENTE_CANCELACION: 3,
        APROBADO: 4,
        ACTIVO: 5,
        FINALIZADO: 6,
        RECHAZADO: 7,
        CANCELADO: 8
      } as const;
      const oa = orden[a.estado as keyof typeof orden] ?? 9;
      const ob = orden[b.estado as keyof typeof orden] ?? 9;
      if (oa !== ob) return oa - ob;
      return new Date(b.fecha).getTime() - new Date(a.fecha).getTime();
    });
    return lista;
  });

  ngOnInit() {
    this.route.queryParamMap.subscribe((qp) => {
      const estado = qp.get('estado') as FiltroEstado | null;
      const focus = qp.get('focus');
      const q = qp.get('q');
      if (estado) this.filtroEstado.set(estado);
      if (focus) this.idFocus.set(Number(focus));
      if (q) this.busqueda.set(q);
    });
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.eventoApi.listar().subscribe({
      next: (lista) => {
        this.eventos.set(lista);
        this.cargando.set(false);
        this.cargarOrganizadores(lista);
      },
      error: () => this.cargando.set(false)
    });
  }

  private cargarOrganizadores(eventos: Evento[]) {
    const ids = Array.from(new Set(eventos.map((e) => e.organizadorId)));
    this.usuarioApi.listarTodos().subscribe({
      next: (todos) => {
        const map = new Map<number, Usuario>();
        for (const id of ids) {
          const u = todos.find((x) => x.id === id);
          if (u) map.set(id, u);
        }
        this.organizadores.set(map);
      }
    });
  }

  nombreOrganizador(id: number): string {
    return this.organizadores().get(id)?.nombre ?? '—';
  }

  emailOrganizador(id: number): string {
    return this.organizadores().get(id)?.email ?? '';
  }

  aprobar(e: Evento) {
    const adminId = this.store.usuario()?.id;
    this.procesando.set(e.id);
    this.eventoApi.aprobar(e.id, adminId).subscribe({
      next: (actualizado) => {
        this.procesando.set(null);
        this.actualizarEvento(actualizado);
        this.cerrarDetalleSiCorresponde(actualizado.id);
        const detail =
          actualizado.estado === 'PENDIENTE_SUPLEMENTO'
            ? `"${actualizado.nombre}": cambios aprobados. El organizador debe subir el comprobante del incremento; luego aprueba el pago en Pagos.`
            : `"${actualizado.nombre}" fue aprobado.`;
        this.messages.add({
          severity: 'success',
          summary:
            actualizado.estado === 'PENDIENTE_SUPLEMENTO' ? 'Cambios aprobados' : 'Evento aprobado',
          detail,
          life: 4500
        });
      },
      error: () => this.procesando.set(null)
    });
  }

  /** Novedad AUMENTO_HORAS pendiente → tras aprobar evento queda PENDIENTE_SUPLEMENTO y cobro solo del delta. */
  tieneSuplementoHorasPendiente(): boolean {
    return this.novedades().some(
      (n) => n.tipo === 'AUMENTO_HORAS' && n.estado === 'PENDIENTE'
    );
  }

  abrirDetalle(e: Evento) {
    this.eventoDetalle.set(e);
    this.mostrarModalDetalle.set(true);
    this.cargandoDetalleModal.set(true);
    this.novedades.set([]);
    this.pagoEvento.set(null);
    this.cargarNovedades(e.id);
    if (e.costo > 0) {
      this.cargarPagoEvento(e.id);
    }
    this.eventoApi.obtener(e.id).subscribe({
      next: (fresh) => {
        this.eventoDetalle.set(fresh);
        this.actualizarEvento(fresh);
        this.cargandoDetalleModal.set(false);
        if (fresh.costo > 0 && !this.pagoEvento()) {
          this.cargarPagoEvento(fresh.id);
        }
      },
      error: () => {
        this.cargandoDetalleModal.set(false);
        this.messages.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudo cargar el detalle del evento.',
          life: 4000
        });
      }
    });
  }

  cerrarDetalle() {
    this.mostrarModalDetalle.set(false);
    this.eventoDetalle.set(null);
    this.cargandoDetalleModal.set(false);
    this.novedades.set([]);
    this.pagoEvento.set(null);
    this.mostrarComprobantePago.set(false);
  }

  private cargarPagoEvento(eventoId: number) {
    this.cargandoPago.set(true);
    this.pagoApi.obtenerPorEvento(eventoId).subscribe({
      next: (p) => {
        this.pagoEvento.set(p ?? null);
        this.cargandoPago.set(false);
      },
      error: () => {
        this.pagoEvento.set(null);
        this.cargandoPago.set(false);
      }
    });
  }

  urlComprobante(p: Pago): string {
    return urlComprobantePago(p);
  }

  urlComprobanteSegura(p: Pago) {
    return urlComprobanteSeguraPago(p, this.sanitizer);
  }

  esImagenComprobante(p: Pago): boolean {
    return esComprobanteImagen(p);
  }

  esPdfComprobante(p: Pago): boolean {
    return esComprobantePdf(p);
  }

  fechaEventoParaSalon(ev: Evento): Date | null {
    return ev.fecha ? new Date(ev.fecha) : null;
  }

  horaInicioParaSalon(ev: Evento): Date | null {
    return ev.fecha ? new Date(ev.fecha) : null;
  }

  horaFinParaSalon(ev: Evento): Date | null {
    if (ev.fechaFin) return new Date(ev.fechaFin);
    if (!ev.fecha) return null;
    const h = ev.duracionHoras && ev.duracionHoras > 0 ? ev.duracionHoras : 1;
    return new Date(new Date(ev.fecha).getTime() + h * 3600000);
  }

  abrirDisponibilidadSalon(evento?: Evento) {
    this.eventoParaSalon.set(evento ?? null);
    this.mostrarDisponibilidadSalon.set(true);
  }

  onSalonVisibleChange(visible: boolean) {
    this.mostrarDisponibilidadSalon.set(visible);
    if (!visible) {
      this.eventoParaSalon.set(null);
    }
  }

  private cargarNovedades(eventoId: number) {
    this.cargandoNovedades.set(true);
    this.eventoApi.listarNovedades(eventoId).subscribe({
      next: (lista) => {
        this.novedades.set(lista);
        this.cargandoNovedades.set(false);
      },
      error: () => {
        this.novedades.set([]);
        this.cargandoNovedades.set(false);
      }
    });
  }

  labelTipoNovedad(t: EventoNovedad['tipo']): string {
    const m: Record<EventoNovedad['tipo'], string> = {
      EDICION_METADATOS: 'Edición datos',
      EDICION_TIPO_CATEGORIA: 'Tipo / categoría',
      AUMENTO_HORAS: 'Aumento de horas',
      DISMINUCION_HORAS: 'Disminución de horas',
      CANCELACION_SOLICITUD: 'Cancelación'
    };
    return m[t] ?? t;
  }

  labelEstadoNovedad(est: EventoNovedad['estado']): string {
    const m: Record<EventoNovedad['estado'], string> = {
      PENDIENTE: 'Pendiente',
      APROBADO: 'Aprobado',
      RECHAZADO: 'Rechazado'
    };
    return m[est] ?? est;
  }

  /** Texto claro para el admin a partir del JSON guardado en backend. */
  detalleNovedadLegible(n: EventoNovedad): string {
    const raw = n.detalleJson?.trim();
    if (!raw) return '';
    try {
      const d = JSON.parse(raw) as Record<string, unknown>;
      const out: string[] = [];
      const ep = d['estadoPrevio'];
      if (typeof ep === 'string' && ep.length > 0) {
        out.push(`Estado del evento antes de la solicitud: ${ep}.`);
      }
      if (n.tipo === 'CANCELACION_SOLICITUD') {
        const motivo = d['motivo'];
        if (typeof motivo === 'string' && motivo.trim()) {
          out.push(`Motivo indicado por el organizador: ${motivo.trim()}`);
        }
        out.push(`Valor pagado a la plataforma: ${this.formatCop(d['valorPagadoPlataforma'])}.`);
        out.push(`Reembolso orientativo al 70%: ${this.formatCop(d['reembolsoPropuesto70'])}.`);
        return out.join('\n');
      }
      if (n.tipo === 'AUMENTO_HORAS') {
        out.push(
          `Duración: ${d['horasAntes'] ?? '—'} h → ${d['horasDespues'] ?? '—'} h. Monto ya aprobado/pagado: ${this.formatCop(d['montoPagadoPrevio'])}; complemento registrado en la solicitud: ${this.formatCop(d['montoAdicional'])}.`
        );
        this.appendEventoAntesLines(out, d['eventoAntes']);
        return out.join('\n');
      }
      if (n.tipo === 'DISMINUCION_HORAS') {
        out.push(
          `Duración: ${d['horasAntes'] ?? '—'} h → ${d['horasDespues'] ?? '—'} h (${d['horasReducidas'] ?? '—'} h menos). Penalización estimada (referencia): ${this.formatCop(d['penalizacionEstimada'])}.`
        );
        this.appendEventoAntesLines(out, d['eventoAntes']);
        return out.join('\n');
      }
      const res = d['resumen'];
      if (typeof res === 'string' && res.trim()) {
        out.push(`Resumen de la solicitud: ${res.trim()}.`);
      }
      out.push('Valores previos a la solicitud (referencia si rechazas y se revierte):');
      this.appendEventoAntesLines(out, d['eventoAntes']);
      return out.join('\n');
    } catch {
      return raw;
    }
  }

  private formatCop(v: unknown): string {
    const n = typeof v === 'number' ? v : Number(v);
    if (Number.isNaN(n)) return '—';
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      maximumFractionDigits: 0
    }).format(n);
  }

  private formatFechaLegible(iso: unknown): string {
    if (typeof iso !== 'string' || !iso.trim()) return '—';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return iso;
    return d.toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' });
  }

  private appendEventoAntesLines(out: string[], raw: unknown): void {
    if (!raw || typeof raw !== 'object') {
      out.push('• (Sin detalle estructurado del evento anterior.)');
      return;
    }
    const ea = raw as Record<string, unknown>;
    const line = (label: string, val: string) => out.push(`• ${label}: ${val}`);
    if (ea['nombre'] != null) line('Nombre', String(ea['nombre']));
    if (ea['descripcion'] != null) {
      const t = String(ea['descripcion']);
      line('Descripción', t.length > 400 ? `${t.slice(0, 400)}…` : t);
    }
    if (ea['fecha'] != null) line('Inicio (antes)', this.formatFechaLegible(ea['fecha']));
    if (ea['fechaFin'] != null) line('Fin (antes)', this.formatFechaLegible(ea['fechaFin']));
    if (ea['ubicacion'] != null) line('Ubicación', String(ea['ubicacion']));
    if (ea['aforoMaximo'] != null) line('Aforo máximo', String(ea['aforoMaximo']));
    if (ea['tipoEvento'] != null) line('Modalidad', String(ea['tipoEvento']));
    if (ea['categoria'] != null) line('Categoría', String(ea['categoria']));
    if (ea['duracionHoras'] != null) line('Duración (h)', String(ea['duracionHoras']));
    if (ea['costo'] != null) line('Costo/tarifa (antes)', this.formatCop(ea['costo']));
    if (ea['imagen'] != null && String(ea['imagen']).trim()) line('Imagen (URL)', String(ea['imagen']));
  }

  abrirRechazoDesdeDetalle() {
    const e = this.eventoDetalle();
    if (!e) return;
    this.mostrarModalDetalle.set(false);
    this.eventoDetalle.set(null);
    this.novedades.set([]);
    this.abrirRechazo(e);
  }

  abrirRechazoCancelacionDesdeDetalle() {
    const e = this.eventoDetalle();
    if (!e) return;
    this.mostrarModalDetalle.set(false);
    this.eventoDetalle.set(null);
    this.novedades.set([]);
    this.abrirRechazoCancelacion(e);
  }

  subtituloModalDetalle(e: Evento): string {
    if (e.estado === 'PENDIENTE') {
      return 'Solicitud pendiente de revisión (alta o cambios del organizador). Revisa todos los datos antes de decidir.';
    }
    if (e.estado === 'PENDIENTE_REVISION') {
      if (this.tieneSuplementoHorasPendiente()) {
        return 'Ampliación de horas: aprueba primero los cambios del evento. Después el organizador subirá el comprobante solo por el incremento y lo validarás en Pagos.';
      }
      return 'Cambios pendientes de aprobación. Si apruebas, el evento vuelve al estado previo sin nuevo cobro (salvo que el resumen indique lo contrario).';
    }
    if (e.estado === 'PENDIENTE_SUPLEMENTO') {
      return 'Cambios de horario ya aprobados: el organizador debe subir el comprobante del incremento. Valídalo y aprueba el pago en la sección Pagos.';
    }
    if (e.estado === 'PENDIENTE_CANCELACION') {
      return 'Solicitud de cancelación: revisa el motivo y el historial de pagos. Si apruebas, el evento queda cancelado (devolución orientativa 70% al organizador).';
    }
    return 'Información completa del evento en la plataforma.';
  }

  imagenEventoAbsoluta(url?: string | null): boolean {
    return !!url && /^https?:\/\//i.test(url.trim());
  }

  abrirRechazo(e: Evento) {
    this.tipoRechazo.set('evento');
    this.eventoARechazar.set(e);
    this.motivoRechazo = '';
    this.mostrarModalRechazo.set(true);
  }

  abrirRechazoCancelacion(e: Evento) {
    this.tipoRechazo.set('cancelacion');
    this.eventoARechazar.set(e);
    this.motivoRechazo = '';
    this.mostrarModalRechazo.set(true);
  }

  aprobarCancelacion(e: Evento) {
    const adminId = this.store.usuario()?.id;
    this.procesando.set(e.id);
    this.eventoApi.aprobarCancelacion(e.id, adminId).subscribe({
      next: (actualizado) => {
        this.procesando.set(null);
        this.actualizarEvento(actualizado);
        this.cerrarDetalleSiCorresponde(actualizado.id);
        this.messages.add({
          severity: 'success',
          summary: 'Cancelación aprobada',
          detail: `"${actualizado.nombre}" quedó cancelado.`,
          life: 4000
        });
      },
      error: () => this.procesando.set(null)
    });
  }

  confirmarRechazo() {
    const e = this.eventoARechazar();
    if (!e) return;
    if (!this.motivoRechazo.trim() || this.motivoRechazo.trim().length < 5) {
      this.messages.add({
        severity: 'warn',
        summary: 'Motivo requerido',
        detail: 'Ingresa un motivo de al menos 5 caracteres.'
      });
      return;
    }
    const adminId = this.store.usuario()?.id;
    const tipo = this.tipoRechazo();
    this.procesando.set(e.id);
    const req =
      tipo === 'cancelacion'
        ? this.eventoApi.rechazarCancelacion(e.id, this.motivoRechazo.trim(), adminId)
        : this.eventoApi.rechazar(e.id, this.motivoRechazo.trim(), adminId);
    req.subscribe({
      next: (actualizado) => {
        this.procesando.set(null);
        this.mostrarModalRechazo.set(false);
        this.actualizarEvento(actualizado);
        this.cerrarDetalleSiCorresponde(actualizado.id);
        const esCancel = tipo === 'cancelacion';
        this.messages.add({
          severity: 'success',
          summary: esCancel ? 'Solicitud de cancelación rechazada' : 'Evento rechazado',
          detail: esCancel
            ? `Se devolvió el evento al estado operativo y se notificó al organizador.`
            : `"${e.nombre}" fue rechazado con el motivo enviado al organizador.`,
          life: 4000
        });
        this.tipoRechazo.set('evento');
      },
      error: () => this.procesando.set(null)
    });
  }

  cerrarRechazo() {
    this.mostrarModalRechazo.set(false);
    this.eventoARechazar.set(null);
    this.tipoRechazo.set('evento');
  }

  private actualizarEvento(e: Evento) {
    this.eventos.update((items) => items.map((x) => (x.id === e.id ? e : x)));
  }

  private cerrarDetalleSiCorresponde(id: number) {
    if (this.eventoDetalle()?.id === id) {
      this.mostrarModalDetalle.set(false);
      this.eventoDetalle.set(null);
      this.cargandoDetalleModal.set(false);
      this.novedades.set([]);
    }
  }
}
