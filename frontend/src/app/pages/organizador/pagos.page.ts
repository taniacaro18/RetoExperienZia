import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { MessageService } from 'primeng/api';
import { forkJoin } from 'rxjs';
import { AuthStore } from '../../core/auth/auth.store';
import { PagoApi } from '../../core/api/pago.api';
import { EventoApi } from '../../core/api/evento.api';
import { EstadoPago, Evento, Pago } from '../../core/models/domain.models';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';
import { pagoEstadoSeverity } from '../../shared/estado.helpers';
import { environment } from '../../../environments/environment';

interface FilaEventoConPago {
  evento: Evento;
  pago?: Pago;
  estadoPago: 'NO_PAGADO' | EstadoPago;
}

@Component({
  selector: 'app-org-pagos-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    CurrencyPipe,
    RouterLink,
    ProgressSpinnerModule,
    TagModule,
    DialogModule,
    StatCardComponent
  ],
  templateUrl: './pagos.page.html'
})
export class OrgPagosPage {
  private readonly store = inject(AuthStore);
  private readonly pagoApi = inject(PagoApi);
  private readonly eventoApi = inject(EventoApi);
  private readonly toast = inject(MessageService);
  private readonly sanitizer = inject(DomSanitizer);

  readonly cargando = signal(true);
  readonly subiendo = signal<number | null>(null);
  readonly eventos = signal<Evento[]>([]);
  readonly pagos = signal<Pago[]>([]);

  readonly mostrarSubir = signal(false);
  readonly eventoSeleccionado = signal<Evento | null>(null);
  archivo: File | null = null;

  readonly mostrarComprobante = signal(false);
  readonly pagoVisible = signal<Pago | null>(null);

  /** Monto que debe reflejar el archivo (tarifa completa o solo incremento si hay complemento). */
  readonly montoModalSubida = computed(() => {
    const ev = this.eventoSeleccionado();
    if (!ev) return 0;
    const p = this.pagos().find((x) => x.eventoId === ev.id);
    if (
      p?.estado === 'PENDIENTE' &&
      p.saldoAprobadoPrevio != null &&
      p.saldoAprobadoPrevio > 0 &&
      (p.monto ?? 0) > 0
    ) {
      return p.monto ?? 0;
    }
    return ev.costo ?? 0;
  });

  readonly estadoSeverity = pagoEstadoSeverity;

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    const orgId = this.store.usuario()?.id;
    if (!orgId) return;
    this.cargando.set(true);
    forkJoin({
      eventos: this.eventoApi.listarPorOrganizador(orgId),
      pagos: this.pagoApi.listarPorOrganizador(orgId)
    }).subscribe({
      next: ({ eventos, pagos }) => {
        this.eventos.set(eventos);
        this.pagos.set(pagos);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
  }

  /**
   * Eventos ya aprobados por admin y visibles en flujo de pago.
   * Se excluyen los de costo $0 (sin tarifa / no requieren comprobante).
   */
  readonly filas = computed<FilaEventoConPago[]>(() => {
    const pagos = this.pagos();
    return this.eventos()
      .filter((e) => {
        if (
          e.estado !== 'APROBADO' &&
          e.estado !== 'ACTIVO' &&
          e.estado !== 'FINALIZADO' &&
          e.estado !== 'PENDIENTE_SUPLEMENTO'
        ) {
          return false;
        }
        const tieneTarifa = (e.costo ?? 0) > 0;
        const tieneHistorialPago = pagos.some((p) => p.eventoId === e.id);
        return tieneTarifa || tieneHistorialPago;
      })
      .map((evento) => {
        const pago = pagos.find((p) => p.eventoId === evento.id);
        const estadoPago: FilaEventoConPago['estadoPago'] = pago ? pago.estado : 'NO_PAGADO';
        return { evento, pago, estadoPago };
      })
      .sort((a, b) => {
        const orden: Record<FilaEventoConPago['estadoPago'], number> = {
          NO_PAGADO: 0,
          RECHAZADO: 1,
          PENDIENTE: 2,
          APROBADO: 3
        };
        return orden[a.estadoPago] - orden[b.estadoPago];
      });
  });

  /** Eventos aún no listos para el flujo de comprobante (alta o cambios en revisión admin). */
  readonly pendientesAdmin = computed(() =>
    this.eventos().filter((e) => e.estado === 'PENDIENTE' || e.estado === 'PENDIENTE_REVISION').length
  );

  readonly conteo = computed(() => {
    const filas = this.filas();
    return {
      total: filas.length,
      pagados: filas.filter((f) => f.estadoPago === 'APROBADO').length,
      pendientes: filas.filter((f) => f.estadoPago === 'PENDIENTE').length,
      faltan: filas.filter(
        (f) =>
          f.estadoPago === 'NO_PAGADO' ||
          f.estadoPago === 'RECHAZADO' ||
          (f.estadoPago === 'PENDIENTE' && f.pago != null && f.pago.saldoAprobadoPrevio != null)
      ).length
    };
  });

  readonly totalPagado = computed(() =>
    this.pagos()
      .filter((p) => p.estado === 'APROBADO')
      .reduce((acc, p) => acc + (p.monto ?? 0), 0)
  );

  readonly totalPendiente = computed(() =>
    this.pagos()
      .filter((p) => p.estado === 'PENDIENTE')
      .reduce((acc, p) => acc + (p.monto ?? 0), 0)
  );

  abrirSubir(evento: Evento) {
    this.eventoSeleccionado.set(evento);
    this.archivo = null;
    this.mostrarSubir.set(true);
  }

  cerrarSubir() {
    this.mostrarSubir.set(false);
    this.eventoSeleccionado.set(null);
    this.archivo = null;
  }

  seleccionarArchivo(e: Event) {
    const input = e.target as HTMLInputElement;
    this.archivo = input.files && input.files[0] ? input.files[0] : null;
  }

  confirmarSubida() {
    const evento = this.eventoSeleccionado();
    const orgId = this.store.usuario()?.id;
    if (!evento || !orgId || !this.archivo) {
      this.toast.add({
        severity: 'warn',
        summary: 'Falta el archivo',
        detail: 'Selecciona el comprobante (PDF o imagen).',
        life: 3500
      });
      return;
    }
    this.subiendo.set(evento.id);
    this.pagoApi.registrar(evento.id, orgId, this.archivo).subscribe({
      next: (p) => {
        this.subiendo.set(null);
        this.toast.add({
          severity: 'info',
          summary: 'Comprobante enviado',
          detail:
            'Tu pago queda pendiente de aprobación por el administrador. Cuando lo apruebe, tu evento pasará a ACTIVO.',
          life: 6000
        });
        const actuales = this.pagos().filter((x) => x.eventoId !== p.eventoId);
        this.pagos.set([p, ...actuales]);
        this.cerrarSubir();
      },
      error: (err) => {
        this.subiendo.set(null);
        this.toast.add({
          severity: 'error',
          summary: 'No se pudo registrar',
          detail: err?.error?.message || 'Intenta nuevamente.',
          life: 5000
        });
      }
    });
  }

  verComprobante(p: Pago) {
    this.pagoVisible.set(p);
    this.mostrarComprobante.set(true);
  }

  urlComprobante(p: Pago): string {
    if (!p.comprobanteUrl) return '';
    if (p.comprobanteUrl.startsWith('http')) return p.comprobanteUrl;
    const path = p.comprobanteUrl.startsWith('/') ? p.comprobanteUrl : '/' + p.comprobanteUrl;
    return environment.apiUrl + path;
  }

  urlComprobanteSegura(p: Pago): SafeResourceUrl {
    return this.sanitizer.bypassSecurityTrustResourceUrl(this.urlComprobante(p));
  }

  esImagen(p: Pago): boolean {
    const u = (p.comprobanteUrl || '').toLowerCase();
    return /\.(png|jpe?g|gif|webp|bmp)$/.test(u);
  }

  esPdf(p: Pago): boolean {
    return (p.comprobanteUrl || '').toLowerCase().endsWith('.pdf');
  }

  badgeEstado(estado: FilaEventoConPago['estadoPago']): string {
    switch (estado) {
      case 'NO_PAGADO': return 'bg-rose-100 text-rose-700 border-rose-200';
      case 'RECHAZADO': return 'bg-rose-100 text-rose-700 border-rose-200';
      case 'PENDIENTE': return 'bg-amber-100 text-amber-700 border-amber-200';
      case 'APROBADO': return 'bg-emerald-100 text-emerald-700 border-emerald-200';
    }
  }

  iconoEstado(estado: FilaEventoConPago['estadoPago']): string {
    switch (estado) {
      case 'NO_PAGADO': return 'pi-exclamation-circle';
      case 'RECHAZADO': return 'pi-times-circle';
      case 'PENDIENTE': return 'pi-clock';
      case 'APROBADO': return 'pi-check-circle';
    }
  }

  textoEstado(estado: FilaEventoConPago['estadoPago']): string {
    switch (estado) {
      case 'NO_PAGADO': return 'Sin pagar';
      case 'RECHAZADO': return 'Rechazado';
      case 'PENDIENTE': return 'Pago pendiente de aprobación';
      case 'APROBADO': return 'Aprobado';
    }
  }
}
