/**
 * Contador de notificaciones no leídas para la campana del menú.
 */
import { Injectable, inject, signal } from '@angular/core';
import { interval, of, switchMap } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { NotificacionApi } from '../api/notificacion.api';
import { AuthStore } from '../auth/auth.store';

@Injectable({ providedIn: 'root' })
export class NotificacionStore {
  private readonly api = inject(NotificacionApi);
  private readonly auth = inject(AuthStore);

  private readonly _noLeidas = signal(0);
  readonly noLeidas = this._noLeidas.asReadonly();

  // Pide al backend y cuenta las que tienen leida = false.
  refrescar() {
    const userId = this.auth.usuario()?.id;
    if (!userId) {
      this._noLeidas.set(0);
      return;
    }
    this.api.listar(userId).subscribe({
      next: (lista) => this._noLeidas.set(lista.filter((n) => !n.leida).length),
      error: () => { /* silencioso */ }
    });
  }

  // Pone el número a mano (ej. después de marcar una como leída).
  setNoLeidas(n: number) {
    this._noLeidas.set(Math.max(0, n));
  }

  // Resta uno al contador.
  decrement() {
    this._noLeidas.update((v) => Math.max(0, v - 1));
  }

  // Refresca al inicio y cada 60 segundos mientras hay sesión.
  iniciarPolling() {
    this.refrescar();
    interval(60000).pipe(
      switchMap(() => {
        const userId = this.auth.usuario()?.id;
        if (!userId) return of([]);
        return this.api.listar(userId).pipe(catchError(() => of([])));
      })
    ).subscribe((lista) => {
      this._noLeidas.set(lista.filter((n) => !n.leida).length);
    });
  }
}
