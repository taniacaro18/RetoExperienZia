import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { TextareaModule } from 'primeng/textarea';
import { MessageService } from 'primeng/api';
import { AuthStore } from '../../core/auth/auth.store';
import { PagoApi } from '../../core/api/pago.api';
import { EstadoPago, Pago } from '../../core/models/domain.models';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';
import { pagoEstadoSeverity } from '../../shared/estado.helpers';
import {
  esComprobanteImagen,
  esComprobantePdf,
  urlComprobantePago,
  urlComprobanteSeguraPago
} from '../../core/utils/comprobante.util';

type FiltroEstado = 'TODOS' | EstadoPago;

@Component({
  selector: 'app-admin-pagos-page',
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
    TextareaModule,
    StatCardComponent
  ],
  templateUrl: './pagos.page.html'
})
export class AdminPagosPage {
  private readonly store = inject(AuthStore);
  private readonly pagoApi = inject(PagoApi);
  private readonly messages = inject(MessageService);
  private readonly sanitizer = inject(DomSanitizer);

  readonly cargando = signal(true);
  readonly pagos = signal<Pago[]>([]);
  readonly filtro = signal<FiltroEstado>('PENDIENTE');
  readonly procesando = signal<number | null>(null);

  readonly mostrarModalRechazo = signal(false);
  readonly pagoARechazar = signal<Pago | null>(null);
  motivoRechazo = '';

  readonly mostrarModalComprobante = signal(false);
  readonly pagoVisible = signal<Pago | null>(null);

  estadoSeverity = pagoEstadoSeverity;

  readonly conteo = computed(() => {
    const items = this.pagos();
    return {
      total: items.length,
      pendientes: items.filter((p) => p.estado === 'PENDIENTE').length,
      aprobados: items.filter((p) => p.estado === 'APROBADO').length,
      rechazados: items.filter((p) => p.estado === 'RECHAZADO').length
    };
  });

  readonly pagosFiltrados = computed(() => {
    const f = this.filtro();
    let lista = [...this.pagos()];
    if (f !== 'TODOS') lista = lista.filter((p) => p.estado === f);
    lista.sort((a, b) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime());
    return lista;
  });

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.pagoApi.listarTodos().subscribe({
      next: (lista) => {
        this.pagos.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
  }

  urlComprobante(p: Pago): string {
    return urlComprobantePago(p);
  }

  urlComprobanteSegura(p: Pago): SafeResourceUrl {
    return urlComprobanteSeguraPago(p, this.sanitizer);
  }

  esImagen(p: Pago): boolean {
    return esComprobanteImagen(p);
  }

  esPdf(p: Pago): boolean {
    return esComprobantePdf(p);
  }

  puedeAprobar(p: Pago): boolean {
    return p.estado === 'PENDIENTE' && !!p.comprobanteUrl?.trim();
  }

  verComprobante(p: Pago) {
    this.pagoVisible.set(p);
    this.mostrarModalComprobante.set(true);
  }

  aprobar(p: Pago) {
    const adminId = this.store.usuario()?.id;
    this.procesando.set(p.id);
    this.pagoApi.aprobar(p.id, adminId).subscribe({
      next: (actualizado) => {
        this.procesando.set(null);
        this.actualizar(actualizado);
        this.messages.add({
          severity: 'success',
          summary: 'Pago aprobado',
          detail: `El evento asociado se activará automáticamente.`,
          life: 3500
        });
      },
      error: () => this.procesando.set(null)
    });
  }

  abrirRechazo(p: Pago) {
    this.pagoARechazar.set(p);
    this.motivoRechazo = '';
    this.mostrarModalRechazo.set(true);
  }

  confirmarRechazo() {
    const p = this.pagoARechazar();
    if (!p) return;
    if (!this.motivoRechazo.trim() || this.motivoRechazo.trim().length < 5) {
      this.messages.add({
        severity: 'warn',
        summary: 'Motivo requerido',
        detail: 'Ingresa un motivo de al menos 5 caracteres.'
      });
      return;
    }
    const adminId = this.store.usuario()?.id;
    this.procesando.set(p.id);
    this.pagoApi.rechazar(p.id, this.motivoRechazo.trim(), adminId).subscribe({
      next: (actualizado) => {
        this.procesando.set(null);
        this.mostrarModalRechazo.set(false);
        this.actualizar(actualizado);
        this.messages.add({
          severity: 'success',
          summary: 'Pago rechazado',
          detail: 'Se notificó al organizador con el motivo.',
          life: 4000
        });
      },
      error: () => this.procesando.set(null)
    });
  }

  cerrarRechazo() {
    this.mostrarModalRechazo.set(false);
    this.pagoARechazar.set(null);
  }

  private actualizar(p: Pago) {
    this.pagos.update((items) => items.map((x) => (x.id === p.id ? p : x)));
  }
}
