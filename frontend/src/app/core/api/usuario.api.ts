import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EstadoUsuario, Rol, Usuario } from '../models/domain.models';

export interface UsuarioSearchCriteria {
  nombre?: string;
  email?: string;
  rol?: Rol;
  estado?: EstadoUsuario;
  organizadorId?: number;
}

export interface CrearStaffPayload {
  organizadorId: number;
  nombre: string;
  email: string;
  password: string;
  telefono?: string;
  tipoDocumento?: string;
  numeroDocumento?: string;
}

@Injectable({ providedIn: 'root' })
export class UsuarioApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl + '/api/usuarios';
  private readonly admin = environment.apiUrl + '/api/admin/usuarios';

  listarTodos(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(this.base);
  }

  buscar(criterios: UsuarioSearchCriteria): Observable<Usuario[]> {
    let params = new HttpParams();
    Object.entries(criterios).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') {
        params = params.set(k, String(v));
      }
    });
    return this.http.get<Usuario[]>(this.base + '/buscar', { params });
  }

  crearStaff(payload: CrearStaffPayload): Observable<Usuario> {
    return this.http.post<Usuario>(this.base + '/staff', payload);
  }

  reenviarCredenciales(id: number, actorId?: number): Observable<{
    usuarioId: number;
    email: string;
    passwordTemporal: string;
    mensaje: string;
  }> {
    const params = actorId ? new HttpParams().set('actorId', String(actorId)) : undefined;
    return this.http.post<any>(this.base + '/' + id + '/reenviar-credenciales', null, { params });
  }

  // Acciones del admin
  aprobarOrganizador(id: number, adminId?: number): Observable<Usuario> {
    const params = adminId ? new HttpParams().set('adminId', String(adminId)) : undefined;
    return this.http.put<Usuario>(this.admin + '/' + id + '/aprobar', null, { params });
  }

  rechazarOrganizador(id: number, adminId?: number): Observable<Usuario> {
    const params = adminId ? new HttpParams().set('adminId', String(adminId)) : undefined;
    return this.http.put<Usuario>(this.admin + '/' + id + '/rechazar', null, { params });
  }

  desactivar(id: number, adminId?: number): Observable<Usuario> {
    const params = adminId ? new HttpParams().set('adminId', String(adminId)) : undefined;
    return this.http.put<Usuario>(this.admin + '/' + id + '/desactivar', null, { params });
  }

  reactivar(id: number, adminId?: number): Observable<Usuario> {
    const params = adminId ? new HttpParams().set('adminId', String(adminId)) : undefined;
    return this.http.put<Usuario>(this.admin + '/' + id + '/reactivar', null, { params });
  }

  cambiarRol(id: number, rol: Rol, adminId?: number): Observable<Usuario> {
    const params = adminId ? new HttpParams().set('adminId', String(adminId)) : undefined;
    return this.http.put<Usuario>(this.admin + '/' + id + '/rol', { rol }, { params });
  }

  // Acciones de organizador sobre su staff
  desactivarStaff(organizadorId: number, staffId: number): Observable<Usuario> {
    return this.http.put<Usuario>(
      environment.apiUrl + '/api/organizadores/' + organizadorId + '/staff/' + staffId + '/desactivar',
      null
    );
  }

  reactivarStaff(organizadorId: number, staffId: number): Observable<Usuario> {
    return this.http.put<Usuario>(
      environment.apiUrl + '/api/organizadores/' + organizadorId + '/staff/' + staffId + '/reactivar',
      null
    );
  }
}
