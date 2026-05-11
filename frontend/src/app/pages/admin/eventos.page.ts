import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { DialogModule } from 'primeng/dialog';
import { MessageService } from 'primeng/api';
import { AuthStore } from '../../core/auth/auth.store';
import { EventoApi } from '../../core/api/evento.api';
import { UsuarioApi } from '../../core/api/usuario.api';
import { EstadoEvento, Evento, Usuario } from '../../core/models/domain.models';
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
    RouterLink,
    ProgressSpinnerModule,
    TagModule,
    InputTextModule,
    TextareaModule,
    DialogModule,
    StatCardComponent,
    AforoBarComponent
  ],
  templateUrl: './eventos.page.html'
})
export class AdminEventosPage {
  private readonly route = inject(ActivatedRoute);
  private readonly store = inject(AuthStore);
  private readonly eventoApi = inject(EventoApi);
  private readonly usuarioApi = inject(UsuarioApi);
  private readonly messages = inject(MessageService);

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
  motivoRechazo = '';

  estadoLabel = eventoEstadoLabel;
  estadoSeverity = eventoEstadoSeverity;

  readonly conteo = computed(() => {
    const items = this.eventos();
    return {
      total: items.length,
      pendientes: items.filter((e) => e.estado === 'PENDIENTE').length,
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
      const orden = { PENDIENTE: 0, APROBADO: 1, ACTIVO: 2, FINALIZADO: 3, RECHAZADO: 4, CANCELADO: 5 } as const;
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
        this.messages.add({
          severity: 'success',
          summary: 'Evento aprobado',
          detail: `"${e.nombre}" fue aprobado.`,
          life: 3500
        });
      },
      error: () => this.procesando.set(null)
    });
  }

  abrirRechazo(e: Evento) {
    this.eventoARechazar.set(e);
    this.motivoRechazo = '';
    this.mostrarModalRechazo.set(true);
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
    this.procesando.set(e.id);
    this.eventoApi.rechazar(e.id, this.motivoRechazo.trim(), adminId).subscribe({
      next: (actualizado) => {
        this.procesando.set(null);
        this.mostrarModalRechazo.set(false);
        this.actualizarEvento(actualizado);
        this.messages.add({
          severity: 'success',
          summary: 'Evento rechazado',
          detail: `"${e.nombre}" fue rechazado con el motivo enviado al organizador.`,
          life: 4000
        });
      },
      error: () => this.procesando.set(null)
    });
  }

  cerrarRechazo() {
    this.mostrarModalRechazo.set(false);
    this.eventoARechazar.set(null);
  }

  private actualizarEvento(e: Evento) {
    this.eventos.update((items) => items.map((x) => (x.id === e.id ? e : x)));
  }
}
