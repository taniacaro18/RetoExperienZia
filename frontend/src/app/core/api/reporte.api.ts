import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DashboardAdmin, DashboardOrganizador } from '../models/domain.models';

export interface AsistenciaReporte {
  eventoId: number;
  totalAsistieron: number;
}

export interface EventoPopular {
  eventoId: number;
  nombre: string;
  totalInscritos: number;
}

export interface ReporteEventoAvanzado {
  eventoId: number;
  nombreEvento: string;
  fechaEvento: string;
  aforoMaximo: number;
  inscritos: number;
  asistieron: number;
  faltaron: number;
  porcentajeOcupacion: number;
  porcentajeAsistencia: number;
  checkInsTotal: number;
  checkInsPorQR: number;
  checkInsManuales: number;
  checkOutsTotal: number;
  curvaIngreso: { hora: number; ingresos: number; salidas: number }[];
  desempenoStaff: {
    staffUsuarioId: number;
    nombre?: string;
    funcion: string;
    checkInsRegistrados: number;
    checkOutsRegistrados: number;
    checkInsPorQR: number;
    checkInsManuales: number;
  }[];
}

@Injectable({ providedIn: 'root' })
export class ReporteApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl + '/api/reportes';

  resumen(): Observable<{ totalUsuarios: number; totalEventos: number; totalInscripciones: number }> {
    return this.http.get<any>(this.base + '/resumen');
  }

  eventosPopulares(): Observable<EventoPopular[]> {
    return this.http.get<EventoPopular[]>(this.base + '/eventos-populares');
  }

  asistenciaPorEvento(eventoId: number): Observable<AsistenciaReporte> {
    return this.http.get<AsistenciaReporte>(this.base + '/asistencia/' + eventoId);
  }

  dashboardOrganizador(organizadorId: number): Observable<DashboardOrganizador> {
    return this.http.get<DashboardOrganizador>(
      this.base + '/dashboard/organizador/' + organizadorId
    );
  }

  dashboardAdmin(): Observable<DashboardAdmin> {
    return this.http.get<DashboardAdmin>(this.base + '/dashboard/admin');
  }

  reporteAvanzado(eventoId: number, organizadorId?: number): Observable<ReporteEventoAvanzado> {
    const params = organizadorId
      ? new HttpParams().set('organizadorId', String(organizadorId))
      : undefined;
    return this.http.get<ReporteEventoAvanzado>(
      this.base + '/evento/' + eventoId + '/avanzado',
      { params }
    );
  }
}
