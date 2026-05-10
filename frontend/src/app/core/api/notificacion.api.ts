import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Notificacion } from '../models/domain.models';

@Injectable({ providedIn: 'root' })
export class NotificacionApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl + '/api/notificaciones';

  listar(usuarioId: number): Observable<Notificacion[]> {
    return this.http.get<Notificacion[]>(this.base + '/' + usuarioId);
  }

  marcarLeida(id: number): Observable<Notificacion> {
    return this.http.put<Notificacion>(this.base + '/' + id + '/leida', null);
  }
}
