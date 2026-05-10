import { Injectable, computed, effect, signal } from '@angular/core';
import { Usuario } from '../models/domain.models';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly _usuario = signal<Usuario | null>(this.cargarSesion());

  readonly usuario = this._usuario.asReadonly();
  readonly autenticado = computed(() => this._usuario() !== null);
  readonly rol = computed(() => this._usuario()?.rol ?? null);
  readonly esAdmin = computed(() => this.rol() === 'ADMIN');
  readonly esOrganizador = computed(() => this.rol() === 'ORGANIZADOR');
  readonly esAsistente = computed(() => this.rol() === 'ASISTENTE');
  readonly esStaff = computed(() => this.rol() === 'STAFF');

  constructor() {
    effect(() => {
      const u = this._usuario();
      if (u) {
        localStorage.setItem(environment.storageKey, JSON.stringify(u));
      } else {
        localStorage.removeItem(environment.storageKey);
      }
    });
  }

  setUsuario(u: Usuario | null) {
    this._usuario.set(u);
  }

  logout() {
    this._usuario.set(null);
  }

  private cargarSesion(): Usuario | null {
    try {
      const raw = localStorage.getItem(environment.storageKey);
      return raw ? (JSON.parse(raw) as Usuario) : null;
    } catch {
      return null;
    }
  }
}
