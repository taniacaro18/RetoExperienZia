import { Injectable, inject, signal } from '@angular/core';
import { interval, of, switchMap } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { NotificacionApi } from '../api/notificacion.api';
import { AuthStore } from '../auth/auth.store';

/**
 * Store ligero para el contador de notificaciones no leídas que se muestra
 * en la campana del header. Se refresca al iniciar sesión y cada 60s.
 */
@Injectable({ providedIn: 'root' })
export class NotificacionStore {
  private readonly api = inject(NotificacionApi);
  private readonly auth = inject(AuthStore);

  private readonly _noLeidas = signal(0);
  readonly noLeidas = this._noLeidas.asReadonly();

  /** Vuelve a pedir al backend el conteo de no leídas. */
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

  setNoLeidas(n: number) {
    this._noLeidas.set(Math.max(0, n));
  }

  decrement() {
    this._noLeidas.update((v) => Math.max(0, v - 1));
  }

  /** Inicializa el polling cada 60s (lo invoca el shell al cargar). */
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
