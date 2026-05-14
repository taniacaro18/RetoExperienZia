import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { Textarea } from 'primeng/textarea';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { AuthStore } from '../../core/auth/auth.store';
import { EventoApi } from '../../core/api/evento.api';
import { EstadoEvento, Evento } from '../../core/models/domain.models';
import { StatCardComponent } from '../../shared/stat-card/stat-card.component';
import { AforoBarComponent } from '../../shared/aforo-bar/aforo-bar.component';
import { eventoEstadoLabel, eventoEstadoSeverity } from '../../shared/estado.helpers';
import { eventoVentanaYaCerro } from '../../shared/evento-catalogo.helpers';

type FiltroEstado = 'TODOS' | EstadoEvento;
type Orden = 'FECHA_DESC' | 'FECHA_ASC' | 'NOMBRE' | 'AFORO';

@Component({
  selector: 'app-org-eventos-lista-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    RouterLink,
    ProgressSpinnerModule,
    TagModule,
    DialogModule,
    Textarea,
    InputTextModule,
    SelectModule,
    ConfirmDialogModule,
    StatCardComponent,
    AforoBarComponent
  ],
  providers: [ConfirmationService],
  templateUrl: './eventos-lista.page.html'
})
export class OrgEventosListaPage {
  private readonly store = inject(AuthStore);
  private readonly api = inject(EventoApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly confirm = inject(ConfirmationService);
  private readonly messages = inject(MessageService);

  readonly cargando = signal(true);
  readonly eventos = signal<Evento[]>([]);
  readonly motivoCancelacion = signal('');
  readonly cancelandoId = signal<number | null>(null);
  readonly procesandoCancelacion = signal(false);

  readonly eventoParaCancelar = computed(() => {
    const id = this.cancelandoId();
    if (id == null) return null;
    return this.eventos().find((x) => x.id === id) ?? null;
  });

  /** Cancelaciones que pasan por el administrador (70% devolución orientativa). */
  readonly cancelacionPasaPorAdmin = computed(() => {
    const e = this.eventoParaCancelar();
    if (!e) return false;
    return (
      e.estado === 'ACTIVO' ||
      e.estado === 'APROBADO' ||
      e.estado === 'PENDIENTE_REVISION' ||
      e.estado === 'PENDIENTE_SUPLEMENTO'
    );
  });

  readonly busqueda = signal('');
  readonly filtroEstado = signal<FiltroEstado>('TODOS');
  readonly orden = signal<Orden>('FECHA_DESC');

  readonly opcionesEstado = [
    { label: 'Todos', value: 'TODOS' },
    { label: 'Activos', value: 'ACTIVO' },
    { label: 'Pend. alta', value: 'PENDIENTE' },
    { label: 'Pend. revisión cambios', value: 'PENDIENTE_REVISION' },
    { label: 'Pend. pago adicional', value: 'PENDIENTE_SUPLEMENTO' },
    { label: 'Pend. cancelación', value: 'PENDIENTE_CANCELACION' },
    { label: 'Pend. pago (aprobado)', value: 'APROBADO' },
    { label: 'Rechazados', value: 'RECHAZADO' },
    { label: 'Cancelados', value: 'CANCELADO' },
    { label: 'Finalizados', value: 'FINALIZADO' }
  ];

  readonly opcionesOrden = [
    { label: 'Más recientes', value: 'FECHA_DESC' },
    { label: 'Más antiguos', value: 'FECHA_ASC' },
    { label: 'Por nombre (A-Z)', value: 'NOMBRE' },
    { label: 'Por % de aforo', value: 'AFORO' }
  ];

  readonly conteo = computed(() => {
    const items = this.eventos();
    return {
      total: items.length,
      activos: items.filter((e) => e.estado === 'ACTIVO').length,
      pendientes: items.filter((e) => e.estado === 'PENDIENTE').length,
      revision: items.filter((e) => e.estado === 'PENDIENTE_REVISION').length,
      suplemento: items.filter((e) => e.estado === 'PENDIENTE_SUPLEMENTO').length,
      finalizados: items.filter((e) => e.estado === 'FINALIZADO').length
    };
  });

  readonly eventosFiltrados = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    const est = this.filtroEstado();
    let lista = [...this.eventos()];
    if (est !== 'TODOS') {
      lista = lista.filter((e) => e.estado === est);
    }
    if (q) {
      lista = lista.filter(
        (e) =>
          e.nombre.toLowerCase().includes(q) ||
          (e.categoria || '').toLowerCase().includes(q) ||
          (e.descripcion || '').toLowerCase().includes(q)
      );
    }
    switch (this.orden()) {
      case 'FECHA_DESC':
        lista.sort((a, b) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime()); break;
      case 'FECHA_ASC':
        lista.sort((a, b) => new Date(a.fecha).getTime() - new Date(b.fecha).getTime()); break;
      case 'NOMBRE':
        lista.sort((a, b) => a.nombre.localeCompare(b.nombre)); break;
      case 'AFORO':
        lista.sort((a, b) => {
          const pa = a.aforoMaximo ? a.aforoActual / a.aforoMaximo : 0;
          const pb = b.aforoMaximo ? b.aforoActual / b.aforoMaximo : 0;
          return pb - pa;
        });
        break;
    }
    return lista;
  });

  estadoLabel = eventoEstadoLabel;
  estadoSeverity = eventoEstadoSeverity;

  ngOnInit() {
    this.route.queryParamMap.subscribe((qp) => {
      const q = qp.get('q');
      if (q) this.busqueda.set(q);
    });
    this.cargar();
  }

  private cargar() {
    const orgId = this.store.usuario()?.id;
    if (!orgId) return;
    this.cargando.set(true);
    this.api.listarPorOrganizador(orgId).subscribe({
      next: (lista) => {
        this.eventos.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
  }

  limpiarFiltros() {
    this.busqueda.set('');
    this.filtroEstado.set('TODOS');
    this.orden.set('FECHA_DESC');
  }

  puedeEditar(e: Evento): boolean {
    if (e.estado === 'FINALIZADO' || e.estado === 'CANCELADO') {
      return false;
    }
    if (
      e.estado === 'PENDIENTE_REVISION' ||
      e.estado === 'PENDIENTE_SUPLEMENTO' ||
      e.estado === 'PENDIENTE_CANCELACION'
    ) {
      return false;
    }
    const porEstado =
      e.estado === 'PENDIENTE' ||
      e.estado === 'APROBADO' ||
      e.estado === 'ACTIVO' ||
      e.estado === 'RECHAZADO';
    if (!porEstado) return false;
    if (e.estado === 'ACTIVO' && eventoVentanaYaCerro(e)) {
      return false;
    }
    return true;
  }

  /** Solo hay paso de comprobante cuando el evento tiene tarifa (> 0). */
  requiereComprobante(e: Evento): boolean {
    return (e.costo ?? 0) > 0;
  }

  puedeCancelar(e: Evento): boolean {
    if (
      e.estado === 'FINALIZADO' ||
      e.estado === 'CANCELADO' ||
      e.estado === 'PENDIENTE_CANCELACION' ||
      e.estado === 'RECHAZADO'
    ) {
      return false;
    }
    if (new Date(e.fecha) <= new Date()) return false;
    return (
      e.estado === 'ACTIVO' ||
      e.estado === 'PENDIENTE' ||
      e.estado === 'APROBADO' ||
      e.estado === 'PENDIENTE_REVISION' ||
      e.estado === 'PENDIENTE_SUPLEMENTO'
    );
  }

  abrirEditar(e: Evento) {
    this.router.navigate(['/organizador/eventos', e.id, 'editar']);
  }

  abrirReporte(e: Evento) {
    this.router.navigate(['/organizador/reportes'], { queryParams: { evento: e.id } });
  }

  pedirCancelacion(e: Evento) {
    this.cancelandoId.set(e.id);
    this.motivoCancelacion.set('');
  }

  cerrarCancelacion() {
    this.cancelandoId.set(null);
    this.motivoCancelacion.set('');
    this.procesandoCancelacion.set(false);
  }

  confirmarCancelacion() {
    const id = this.cancelandoId();
    const orgId = this.store.usuario()?.id;
    const motivo = this.motivoCancelacion().trim();
    if (!id || !orgId) return;
    if (motivo.length < 5) {
      this.messages.add({
        severity: 'warn',
        summary: 'Motivo requerido',
        detail: 'Indica un motivo claro (mínimo 5 caracteres).'
      });
      return;
    }
    this.procesandoCancelacion.set(true);
    this.api.cancelar(id, orgId, motivo).subscribe({
      next: (ev) => {
        const pasaAdmin = ev.estado === 'PENDIENTE_CANCELACION';
        this.messages.add({
          severity: pasaAdmin ? 'info' : 'success',
          summary: pasaAdmin ? 'Solicitud enviada' : 'Evento cancelado',
          detail:
            ev.alertaNegocio?.trim() ||
            (pasaAdmin
              ? 'Un administrador revisará la cancelación. Si se aprueba, la devolución orientativa es del 70% del valor pagado.'
              : 'Se notificó a los inscritos.')
        });
        this.cerrarCancelacion();
        this.cargar();
      },
      error: () => this.procesandoCancelacion.set(false)
    });
  }
}
