/**
 * Llama al backend de usuarios (listar, staff, acciones de admin).
 */
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EstadoUsuario, Rol, Usuario } from '../models/domain.models';

// Filtros para buscar usuarios en el panel admin.
export interface UsuarioSearchCriteria {
  nombre?: string;
  email?: string;
  rol?: Rol;
  estado?: EstadoUsuario;
  organizadorId?: number;
}

// Datos para crear un usuario staff del organizador.
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

  // Lista todos los usuarios.
  listarTodos(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(this.base);
  }

  // Busca usuarios con filtros.
  buscar(criterios: UsuarioSearchCriteria): Observable<Usuario[]> {
    let params = new HttpParams();
    Object.entries(criterios).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') {
        params = params.set(k, String(v));
      }
    });
    return this.http.get<Usuario[]>(this.base + '/buscar', { params });
  }

  // El organizador crea un miembro de su equipo staff.
  crearStaff(payload: CrearStaffPayload): Observable<Usuario> {
    return this.http.post<Usuario>(this.base + '/staff', payload);
  }

  // Envía de nuevo la contraseña temporal por email.
  reenviarCredenciales(id: number, actorId?: number): Observable<{
    usuarioId: number;
    email: string;
    passwordTemporal: string;
    mensaje: string;
  }> {
    const params = actorId ? new HttpParams().set('actorId', String(actorId)) : undefined;
    return this.http.post<any>(this.base + '/' + id + '/reenviar-credenciales', null, { params });
  }

  // Admin aprueba un organizador pendiente.
  aprobarOrganizador(id: number, adminId?: number): Observable<Usuario> {
    const params = adminId ? new HttpParams().set('adminId', String(adminId)) : undefined;
    return this.http.put<Usuario>(this.admin + '/' + id + '/aprobar', null, { params });
  }

  // Admin rechaza solicitud de organizador.
  rechazarOrganizador(id: number, adminId?: number): Observable<Usuario> {
    const params = adminId ? new HttpParams().set('adminId', String(adminId)) : undefined;
    return this.http.put<Usuario>(this.admin + '/' + id + '/rechazar', null, { params });
  }

  // Admin desactiva cuenta.
  desactivar(id: number, adminId?: number): Observable<Usuario> {
    const params = adminId ? new HttpParams().set('adminId', String(adminId)) : undefined;
    return this.http.put<Usuario>(this.admin + '/' + id + '/desactivar', null, { params });
  }

  // Admin reactiva cuenta.
  reactivar(id: number, adminId?: number): Observable<Usuario> {
    const params = adminId ? new HttpParams().set('adminId', String(adminId)) : undefined;
    return this.http.put<Usuario>(this.admin + '/' + id + '/reactivar', null, { params });
  }

  // Admin cambia el rol de un usuario.
  cambiarRol(id: number, rol: Rol, adminId?: number): Observable<Usuario> {
    const params = adminId ? new HttpParams().set('adminId', String(adminId)) : undefined;
    return this.http.put<Usuario>(this.admin + '/' + id + '/rol', { rol }, { params });
  }

  // Organizador desactiva a un staff suyo.
  desactivarStaff(organizadorId: number, staffId: number): Observable<Usuario> {
    return this.http.put<Usuario>(
      environment.apiUrl + '/api/organizadores/' + organizadorId + '/staff/' + staffId + '/desactivar',
      null
    );
  }

  // Organizador reactiva a un staff suyo.
  reactivarStaff(organizadorId: number, staffId: number): Observable<Usuario> {
    return this.http.put<Usuario>(
      environment.apiUrl + '/api/organizadores/' + organizadorId + '/staff/' + staffId + '/reactivar',
      null
    );
  }
}
