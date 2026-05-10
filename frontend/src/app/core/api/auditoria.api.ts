import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Auditoria } from '../models/domain.models';

@Injectable({ providedIn: 'root' })
export class AuditoriaApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl + '/api/auditoria';

  listarTodo(): Observable<Auditoria[]> {
    return this.http.get<Auditoria[]>(this.base);
  }

  listarPorUsuario(usuarioId: number): Observable<Auditoria[]> {
    return this.http.get<Auditoria[]>(this.base + '/usuario/' + usuarioId);
  }

  listarPorEntidad(tipo: string): Observable<Auditoria[]> {
    return this.http.get<Auditoria[]>(this.base + '/entidad/' + tipo);
  }
}
