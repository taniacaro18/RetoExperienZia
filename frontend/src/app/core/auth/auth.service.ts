import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ActualizarPerfil,
  LoginRequest,
  LoginResponse,
  Usuario
} from '../models/domain.models';
import { AuthStore } from './auth.store';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly store = inject(AuthStore);
  private readonly base = `${environment.apiUrl}/api/usuarios`;

  login(req: LoginRequest): Observable<Usuario> {
    return this.http.post<LoginResponse>(`${this.base}/login`, req).pipe(
      tap((r) => this.store.setSesion(r.usuario, r.accessToken)),
      map((r) => r.usuario)
    );
  }

  registrar(payload: Partial<Usuario>): Observable<Usuario> {
    return this.http.post<Usuario>(`${this.base}/registro`, payload);
  }

  recuperar(email: string, numeroDocumento: string): Observable<{
    usuarioId: number;
    email: string;
    passwordTemporal: string;
    mensaje: string;
  }> {
    return this.http.post<any>(`${this.base}/recuperar`, {
      email,
      numeroDocumento
    });
  }

  actualizarPerfil(id: number, dto: ActualizarPerfil): Observable<Usuario> {
    return this.http
      .put<Usuario>(`${this.base}/${id}`, dto)
      .pipe(
        tap((u) => {
          if (this.store.usuario()?.id === u.id) {
            this.store.setUsuario(u);
          }
        })
      );
  }

  obtener(id: number): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.base}/${id}`);
  }

  logout() {
    this.store.logout();
  }
}
