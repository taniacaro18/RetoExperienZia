import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { InscripcionApi } from '../../core/api/inscripcion.api';
import { EventoApi } from '../../core/api/evento.api';
import { AuthStore } from '../../core/auth/auth.store';
import { Evento, Inscripcion } from '../../core/models/domain.models';
import { forkJoin } from 'rxjs';
import * as QRCode from 'qrcode';

interface InscripcionConEvento {
  inscripcion: Inscripcion;
  evento: Evento | null;
}

@Component({
  selector: 'app-mis-inscripciones-page',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    ButtonModule,
    TagModule,
    CardModule,
    DialogModule,
    ProgressSpinnerModule,
    ConfirmDialogModule
  ],
  templateUrl: './mis-inscripciones.page.html'
})
export class MisInscripcionesPage {
  private readonly inscripcionesApi = inject(InscripcionApi);
  private readonly eventosApi = inject(EventoApi);
  private readonly auth = inject(AuthStore);
  private readonly messages = inject(MessageService);
  private readonly confirma = inject(ConfirmationService);
  private readonly router = inject(Router);

  readonly cargando = signal(true);
  readonly items = signal<InscripcionConEvento[]>([]);
  readonly qrAbierto = signal<InscripcionConEvento | null>(null);
  readonly qrDataUrl = signal<string | null>(null);
  /** Thumbnails QR pre-generados por inscripción (clave = inscripcionId). */
  readonly qrThumbs = signal<Record<number, string>>({});

  readonly proximas = computed(() => this.items().filter(i =>
    i.evento && new Date(i.evento.fecha).getTime() >= Date.now() && i.inscripcion.estado !== 'CANCELADO'
  ));

  readonly pasadas = computed(() => this.items().filter(i =>
    !i.evento || new Date(i.evento.fecha).getTime() < Date.now() || i.inscripcion.estado === 'ASISTIO'
  ));

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    const u = this.auth.usuario();
    if (!u) return;
    this.cargando.set(true);
    this.inscripcionesApi.listarPorUsuario(u.id).subscribe({
      next: (lista) => {
        if (lista.length === 0) {
          this.items.set([]);
          this.cargando.set(false);
          return;
        }
        const calls = lista.map((ins) => this.eventosApi.obtener(ins.eventoId));
        forkJoin(calls).subscribe({
          next: (eventos) => {
            const combinados = lista.map((inscripcion, i) => ({
              inscripcion,
              evento: eventos[i] ?? null
            }));
            // Ordenar por fecha de evento desc.
            combinados.sort((a, b) => {
              const fa = a.evento ? new Date(a.evento.fecha).getTime() : 0;
              const fb = b.evento ? new Date(b.evento.fecha).getTime() : 0;
              return fb - fa;
            });
            this.items.set(combinados);
            this.cargando.set(false);
            void this.precomputarThumbsQR(combinados);
          },
          error: () => this.cargando.set(false)
        });
      },
      error: () => this.cargando.set(false)
    });
  }

  /** Genera thumbnails QR para todas las inscripciones (no canceladas). */
  private async precomputarThumbsQR(lista: InscripcionConEvento[]) {
    const map: Record<number, string> = {};
    for (const item of lista) {
      const codigo = item.inscripcion.codigoQR;
      if (!codigo || item.inscripcion.estado === 'CANCELADO') continue;
      try {
        map[item.inscripcion.id] = await QRCode.toDataURL(codigo, {
          width: 168,
          margin: 1,
          color: { dark: '#6D28D9', light: '#FFFFFF' }
        });
      } catch {
        // Si alguno falla seguimos con los demás.
      }
    }
    this.qrThumbs.set(map);
  }

  thumbQR(id: number): string | undefined {
    return this.qrThumbs()[id];
  }

  async verQR(item: InscripcionConEvento) {
    const codigo = item.inscripcion.codigoQR;
    if (!codigo) {
      this.messages.add({
        severity: 'warn',
        summary: 'Sin QR',
        detail: 'Esta inscripción aún no tiene código QR.'
      });
      return;
    }
    try {
      const dataUrl = await QRCode.toDataURL(codigo, {
        width: 360,
        margin: 1,
        color: { dark: '#6D28D9', light: '#FFFFFF' }
      });
      this.qrDataUrl.set(dataUrl);
      this.qrAbierto.set(item);
    } catch {
      this.messages.add({
        severity: 'error',
        summary: 'Error',
        detail: 'No se pudo generar el código QR.'
      });
    }
  }

  cerrarQR() {
    this.qrAbierto.set(null);
    this.qrDataUrl.set(null);
  }

  cancelar(item: InscripcionConEvento) {
    this.confirma.confirm({
      message: '¿Cancelar tu inscripción a "' + (item.evento?.nombre ?? 'este evento') + '"?',
      header: 'Confirmar',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Sí, cancelar',
      rejectLabel: 'No',
      acceptButtonStyleClass: '!bg-red-600 !border-red-600',
      accept: () => {
        this.inscripcionesApi.cancelarInscripcion(item.inscripcion.id).subscribe({
          next: () => {
            this.messages.add({
              severity: 'success',
              summary: 'Inscripción cancelada',
              detail: 'Tu cupo fue liberado.'
            });
            this.cargar();
          }
        });
      }
    });
  }

  estadoSeverity(estado: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    switch (estado) {
      case 'ASISTIO': return 'success';
      case 'INSCRITO': return 'info';
      case 'CANCELADO': return 'danger';
      default: return 'secondary';
    }
  }

  descargarQR() {
    const dataUrl = this.qrDataUrl();
    const item = this.qrAbierto();
    if (!dataUrl || !item) return;
    const a = document.createElement('a');
    a.href = dataUrl;
    a.download = `qr-${item.evento?.nombre ?? 'evento'}-${item.inscripcion.id}.png`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }
}
