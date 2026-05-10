import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { AuthStore } from '../../core/auth/auth.store';
import { EventoApi } from '../../core/api/evento.api';
import { CertificadoApi } from '../../core/api/certificado.api';
import { InscripcionApi } from '../../core/api/inscripcion.api';
import { Certificado, Evento } from '../../core/models/domain.models';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';
import { forkJoin } from 'rxjs';

interface EventoConCerts {
  evento: Evento;
  asistencias: number;
  certificados: number;
  estado: 'EN_CURSO' | 'LISTO' | 'ENVIADO' | 'PARCIAL';
}

@Component({
  selector: 'app-org-certificados-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    ProgressSpinnerModule,
    TagModule,
    DialogModule,
    ConfirmDialogModule,
    StatCardComponent
  ],
  providers: [ConfirmationService],
  templateUrl: './certificados.page.html'
})
export class OrgCertificadosPage {
  private readonly store = inject(AuthStore);
  private readonly eventoApi = inject(EventoApi);
  private readonly certApi = inject(CertificadoApi);
  private readonly inscripcionApi = inject(InscripcionApi);
  private readonly messages = inject(MessageService);
  private readonly confirm = inject(ConfirmationService);

  readonly cargando = signal(true);
  readonly procesando = signal<number | null>(null);
  readonly eventosConCerts = signal<EventoConCerts[]>([]);

  readonly modalDetalle = signal<EventoConCerts | null>(null);
  readonly cargandoDetalle = signal(false);
  readonly certificadosDetalle = signal<Certificado[]>([]);

  readonly stats = computed(() => {
    const items = this.eventosConCerts();
    return {
      enCurso: items.filter((i) => i.estado === 'EN_CURSO').length,
      listos: items.filter((i) => i.estado === 'LISTO' || i.estado === 'PARCIAL').length,
      enviados: items.filter((i) => i.estado === 'ENVIADO').length
    };
  });

  ngOnInit() {
    this.cargar();
  }

  private cargar() {
    const orgId = this.store.usuario()?.id;
    if (!orgId) return;
    this.cargando.set(true);

    this.eventoApi.listarPorOrganizador(orgId).subscribe({
      next: (lista) => {
        const candidatos = lista.filter(
          (e) => e.estado === 'ACTIVO' || e.estado === 'FINALIZADO'
        );
        if (candidatos.length === 0) {
          this.eventosConCerts.set([]);
          this.cargando.set(false);
          return;
        }
        const peticionesIns = candidatos.map((e) => this.inscripcionApi.listarPorEvento(e.id));
        const peticionesCert = candidatos.map((e) => this.certApi.listarPorEvento(e.id));

        forkJoin([forkJoin(peticionesIns), forkJoin(peticionesCert)]).subscribe({
          next: ([inscripcionesArr, certificadosArr]) => {
            const items: EventoConCerts[] = candidatos.map((e, i) => {
              const inscripciones = inscripcionesArr[i] ?? [];
              const certificados = certificadosArr[i] ?? [];
              const asistencias = inscripciones.filter((x) => x.estado === 'ASISTIO').length;
              let estado: EventoConCerts['estado'] = 'EN_CURSO';
              if (e.estado === 'FINALIZADO' || new Date(e.fecha) < new Date()) {
                if (asistencias === 0) estado = 'EN_CURSO';
                else if (certificados.length === 0) estado = 'LISTO';
                else if (certificados.length >= asistencias) estado = 'ENVIADO';
                else estado = 'PARCIAL';
              }
              return { evento: e, asistencias, certificados: certificados.length, estado };
            });
            this.eventosConCerts.set(
              items.sort((a, b) => new Date(b.evento.fecha).getTime() - new Date(a.evento.fecha).getTime())
            );
            this.cargando.set(false);
          },
          error: () => this.cargando.set(false)
        });
      },
      error: () => this.cargando.set(false)
    });
  }

  generarMasivo(item: EventoConCerts, ev?: MouseEvent) {
    ev?.stopPropagation();
    const orgId = this.store.usuario()?.id;
    if (!orgId) return;
    this.confirm.confirm({
      header: 'Generar certificados',
      message: `Se generarán certificados para los ${item.asistencias} asistentes confirmados de "${item.evento.nombre}". ¿Continuar?`,
      icon: 'pi pi-verified',
      acceptLabel: 'Generar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: '!bg-brand-600 !border-brand-600',
      accept: () => {
        this.procesando.set(item.evento.id);
        this.certApi.generarMasivo(item.evento.id, orgId).subscribe({
          next: (lista) => {
            this.procesando.set(null);
            this.messages.add({
              severity: 'success',
              summary: 'Certificados generados',
              detail: `${lista.length} certificados emitidos correctamente.`,
              life: 4500
            });
            this.cargar();
          },
          error: (err) => {
            this.procesando.set(null);
            const detalle = err?.error?.message
              || 'No fue posible generar los certificados. Verifica que el evento esté finalizado y tenga asistencias.';
            this.messages.add({
              severity: 'error',
              summary: 'Error al generar',
              detail: detalle,
              life: 6000
            });
          }
        });
      }
    });
  }

  abrirDetalle(item: EventoConCerts) {
    if (item.certificados === 0) {
      this.messages.add({
        severity: 'info',
        summary: 'Sin certificados',
        detail: 'Aún no hay certificados emitidos para este evento.'
      });
      return;
    }
    this.modalDetalle.set(item);
    this.cargandoDetalle.set(true);
    this.certificadosDetalle.set([]);
    this.certApi.listarPorEvento(item.evento.id).subscribe({
      next: (lista) => {
        this.certificadosDetalle.set(lista);
        this.cargandoDetalle.set(false);
      },
      error: () => this.cargandoDetalle.set(false)
    });
  }
  cerrarDetalle() {
    this.modalDetalle.set(null);
    this.certificadosDetalle.set([]);
  }

  exportarListado() {
    const item = this.modalDetalle();
    const certs = this.certificadosDetalle();
    if (!item || certs.length === 0) return;
    const lineas: string[] = ['nombre,documento,codigo,fechaGeneracion'];
    certs.forEach((c) =>
      lineas.push([
        this.csv(c.nombreAsistente ?? ''),
        this.csv(c.numeroDocumento ?? ''),
        this.csv(c.codigo ?? ''),
        this.csv(c.fechaGeneracion ?? '')
      ].join(','))
    );
    const blob = new Blob(['\ufeff' + lineas.join('\n')], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `certificados-${item.evento.nombre.replace(/\s+/g, '_')}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  private csv(v: string): string {
    if (!v) return '';
    const necesita = /[",\n]/.test(v);
    const escape = v.replace(/"/g, '""');
    return necesita ? `"${escape}"` : escape;
  }

  estadoLabel(e: EventoConCerts['estado']): string {
    switch (e) {
      case 'EN_CURSO': return 'En curso';
      case 'LISTO': return 'Listo para emitir';
      case 'PARCIAL': return 'Parcial';
      case 'ENVIADO': return 'Enviados';
    }
  }

  estadoSeverity(e: EventoConCerts['estado']): 'info' | 'warn' | 'success' | 'secondary' {
    switch (e) {
      case 'EN_CURSO': return 'info';
      case 'LISTO': return 'warn';
      case 'PARCIAL': return 'warn';
      case 'ENVIADO': return 'success';
    }
  }

  progreso(item: EventoConCerts): number {
    if (item.asistencias === 0) return 0;
    return Math.min(100, Math.round((item.certificados / item.asistencias) * 100));
  }
}
