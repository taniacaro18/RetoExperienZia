import { Injectable, computed, effect, signal } from '@angular/core';
import { Usuario } from '../models/domain.models';
import { environment } from '../../../environments/environment';

interface SesionAlmacenada {
  usuario: Usuario;
  accessToken: string;
}

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly inicial = this.cargarSesion();
  private readonly _usuario = signal<Usuario | null>(this.inicial?.usuario ?? null);
  private readonly _accessToken = signal<string | null>(this.inicial?.accessToken ?? null);

  readonly usuario = this._usuario.asReadonly();
  readonly accessToken = this._accessToken.asReadonly();
  readonly autenticado = computed(
    () => this._usuario() !== null && this._accessToken() !== null
  );
  readonly rol = computed(() => this._usuario()?.rol ?? null);
  readonly esAdmin = computed(() => this.rol() === 'ADMIN');
  readonly esOrganizador = computed(() => this.rol() === 'ORGANIZADOR');
  readonly esAsistente = computed(() => this.rol() === 'ASISTENTE');
  readonly esStaff = computed(() => this.rol() === 'STAFF');

  constructor() {
    effect(() => {
      const u = this._usuario();
      const t = this._accessToken();
      if (u && t) {
        const payload: SesionAlmacenada = { usuario: u, accessToken: t };
        localStorage.setItem(environment.storageKey, JSON.stringify(payload));
      } else {
        localStorage.removeItem(environment.storageKey);
      }
    });
  }

  setSesion(usuario: Usuario, accessToken: string) {
    this._usuario.set(usuario);
    this._accessToken.set(accessToken);
  }

  /** Actualiza el usuario logueado (p. ej. tras editar perfil); mantiene el token. */
  setUsuario(u: Usuario | null) {
    if (u === null) {
      this.logout();
      return;
    }
    this._usuario.set(u);
  }

  logout() {
    this._usuario.set(null);
    this._accessToken.set(null);
  }

  private cargarSesion(): SesionAlmacenada | null {
    try {
      const raw = localStorage.getItem(environment.storageKey);
      if (!raw) {
        return null;
      }
      const parsed = JSON.parse(raw) as unknown;
      if (
        parsed &&
        typeof parsed === 'object' &&
        'accessToken' in parsed &&
        'usuario' in parsed
      ) {
        const p = parsed as SesionAlmacenada;
        if (p.accessToken && p.usuario) {
          return p;
        }
      }
      // Formato antiguo (solo usuario): obligar a iniciar sesión de nuevo
      localStorage.removeItem(environment.storageKey);
      return null;
    } catch {
      localStorage.removeItem(environment.storageKey);
      return null;
    }
  }
}
