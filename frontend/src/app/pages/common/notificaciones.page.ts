import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageService } from 'primeng/api';
import { AuthStore } from '../../core/auth/auth.store';
import { NotificacionApi } from '../../core/api/notificacion.api';
import { NotificacionStore } from '../../core/state/notificacion.store';
import { Notificacion } from '../../core/models/domain.models';

type FiltroNotif = 'TODAS' | 'NO_LEIDAS' | 'LEIDAS';

@Component({
  selector: 'app-notificaciones-page',
  standalone: true,
  imports: [CommonModule, DatePipe, ProgressSpinnerModule],
  templateUrl: './notificaciones.page.html'
})
export class NotificacionesPage {
  private readonly store = inject(AuthStore);
  private readonly api = inject(NotificacionApi);
  private readonly notifStore = inject(NotificacionStore);
  private readonly toast = inject(MessageService);

  readonly cargando = signal(true);
  readonly notificaciones = signal<Notificacion[]>([]);
  readonly filtro = signal<FiltroNotif>('TODAS');

  readonly conteo = computed(() => {
    const items = this.notificaciones();
    return {
      total: items.length,
      noLeidas: items.filter((n) => !n.leida).length,
      leidas: items.filter((n) => n.leida).length
    };
  });

  readonly filtradas = computed(() => {
    const f = this.filtro();
    return this.notificaciones().filter((n) => {
      if (f === 'NO_LEIDAS') return !n.leida;
      if (f === 'LEIDAS') return n.leida;
      return true;
    });
  });

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    const userId = this.store.usuario()?.id;
    if (!userId) {
      this.cargando.set(false);
      return;
    }
    this.cargando.set(true);
    this.api.listar(userId).subscribe({
      next: (lista) => {
        this.notificaciones.set(lista);
        this.cargando.set(false);
        this.notifStore.setNoLeidas(lista.filter((n) => !n.leida).length);
      },
      error: () => this.cargando.set(false)
    });
  }

  marcarLeida(n: Notificacion) {
    if (n.leida) return;
    this.api.marcarLeida(n.id).subscribe({
      next: (actualizada) => {
        this.notificaciones.update((arr) =>
          arr.map((x) => (x.id === n.id ? actualizada : x))
        );
        this.notifStore.decrement();
      }
    });
  }

  marcarTodasLeidas() {
    const pendientes = this.notificaciones().filter((n) => !n.leida);
    if (pendientes.length === 0) return;
    pendientes.forEach((n) => {
      this.api.marcarLeida(n.id).subscribe({
        next: (actualizada) => {
          this.notificaciones.update((arr) =>
            arr.map((x) => (x.id === n.id ? actualizada : x))
          );
        }
      });
    });
    this.notifStore.setNoLeidas(0);
    this.toast.add({
      severity: 'success',
      summary: 'Listo',
      detail: 'Todas las notificaciones quedaron marcadas como leídas.'
    });
  }

  setFiltro(f: FiltroNotif) {
    this.filtro.set(f);
  }

  iconoTipo(tipo: string): string {
    switch (tipo) {
      case 'ALERTA': return 'pi-exclamation-triangle';
      case 'ERROR':  return 'pi-times-circle';
      default:       return 'pi-info-circle';
    }
  }

  tonoTipo(tipo: string): string {
    switch (tipo) {
      case 'ALERTA': return 'bg-amber-100 text-amber-700 border-amber-200';
      case 'ERROR':  return 'bg-rose-100 text-rose-700 border-rose-200';
      default:       return 'bg-brand-100 text-brand-700 border-brand-200';
    }
  }
}
