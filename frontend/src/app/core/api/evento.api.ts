import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DisponibilidadSalon, EstadoEvento, Evento, EventoNovedad, TipoEvento } from '../models/domain.models';

export interface ConsultaDisponibilidadSalonParams {
  ubicacion?: string;
  desde: string;
  hasta: string;
  excluirEventoId?: number;
  propuestaInicio?: string;
  propuestaFin?: string;
}

export interface EventoSearchCriteria {
  nombre?: string;
  categoria?: string;
  tipoEvento?: TipoEvento;
  estado?: EstadoEvento;
  fechaDesde?: string;
  fechaHasta?: string;
  organizadorId?: number;
}

@Injectable({ providedIn: 'root' })
export class EventoApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl + '/api/eventos';

  listar(): Observable<Evento[]> {
    return this.http.get<Evento[]>(this.base);
  }

  obtener(id: number): Observable<Evento> {
    return this.http.get<Evento>(this.base + '/' + id);
  }

  catalogoPublicos(): Observable<Evento[]> {
    return this.http.get<Evento[]>(this.base + '/catalogo/publicos');
  }

  obtenerPublico(id: number): Observable<Evento> {
    return this.http.get<Evento>(this.base + '/catalogo/publicos/' + id);
  }

  listarPorOrganizador(organizadorId: number): Observable<Evento[]> {
    return this.http.get<Evento[]>(this.base + '/organizador/' + organizadorId);
  }

  crear(payload: Partial<Evento>): Observable<Evento> {
    return this.http.post<Evento>(this.base, payload);
  }

  editar(id: number, payload: Partial<Evento>): Observable<Evento> {
    return this.http.put<Evento>(this.base + '/' + id, payload);
  }

  buscar(criterios: EventoSearchCriteria): Observable<Evento[]> {
    let params = new HttpParams();
    Object.entries(criterios).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') {
        params = params.set(k, String(v));
      }
    });
    return this.http.get<Evento[]>(this.base + '/buscar', { params });
  }

  aprobar(id: number, adminId?: number): Observable<Evento> {
    const params = adminId
      ? new HttpParams().set('adminId', String(adminId))
      : undefined;
    return this.http.post<Evento>(this.base + '/' + id + '/aprobar', null, { params });
  }

  rechazar(id: number, motivo: string, adminId?: number): Observable<Evento> {
    const params = adminId
      ? new HttpParams().set('adminId', String(adminId))
      : undefined;
    return this.http.post<Evento>(this.base + '/' + id + '/rechazar', { motivo }, { params });
  }

  cancelar(id: number, organizadorId: number, motivo: string): Observable<Evento> {
    return this.http.post<Evento>(this.base + '/' + id + '/cancelar', {
      organizadorId,
      motivo
    });
  }

  listarNovedades(id: number): Observable<EventoNovedad[]> {
    return this.http.get<EventoNovedad[]>(this.base + '/' + id + '/novedades');
  }

  consultarDisponibilidadSalon(params: ConsultaDisponibilidadSalonParams): Observable<DisponibilidadSalon> {
    let httpParams = new HttpParams()
      .set('desde', params.desde)
      .set('hasta', params.hasta);
    if (params.ubicacion) httpParams = httpParams.set('ubicacion', params.ubicacion);
    if (params.excluirEventoId != null) {
      httpParams = httpParams.set('excluirEventoId', String(params.excluirEventoId));
    }
    if (params.propuestaInicio) httpParams = httpParams.set('propuestaInicio', params.propuestaInicio);
    if (params.propuestaFin) httpParams = httpParams.set('propuestaFin', params.propuestaFin);
    return this.http.get<DisponibilidadSalon>(this.base + '/salon/disponibilidad', { params: httpParams });
  }

  aprobarCancelacion(id: number, adminId?: number): Observable<Evento> {
    const params = adminId ? new HttpParams().set('adminId', String(adminId)) : undefined;
    return this.http.post<Evento>(this.base + '/' + id + '/cancelacion/aprobar', null, { params });
  }

  rechazarCancelacion(id: number, motivo: string, adminId?: number): Observable<Evento> {
    const params = adminId ? new HttpParams().set('adminId', String(adminId)) : undefined;
    return this.http.post<Evento>(this.base + '/' + id + '/cancelacion/rechazar', { motivo }, { params });
  }
}
