import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Certificado } from '../models/domain.models';

@Injectable({ providedIn: 'root' })
export class CertificadoApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl + '/api/certificados';

  generar(inscripcionId: number): Observable<Certificado> {
    return this.http.post<Certificado>(this.base + '/generar/' + inscripcionId, null);
  }

  listarPorUsuario(usuarioId: number): Observable<Certificado[]> {
    return this.http.get<Certificado[]>(this.base + '/usuario/' + usuarioId);
  }

  validar(codigo: string): Observable<Certificado> {
    return this.http.get<Certificado>(this.base + '/validar/' + codigo);
  }

  generarMasivo(eventoId: number, organizadorId?: number): Observable<Certificado[]> {
    const params = organizadorId
      ? new HttpParams().set('organizadorId', String(organizadorId))
      : undefined;
    return this.http.post<Certificado[]>(
      this.base + '/evento/' + eventoId + '/generar-masivo',
      null,
      { params }
    );
  }

  listarPorEvento(eventoId: number): Observable<Certificado[]> {
    return this.http.get<Certificado[]>(this.base + '/evento/' + eventoId);
  }
}
