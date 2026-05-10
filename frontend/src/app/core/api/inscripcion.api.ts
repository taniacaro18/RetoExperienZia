import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AforoEnVivo,
  AsistenteEvento,
  EventoStaff,
  FuncionStaff,
  Inscripcion,
  StaffAsignado
} from '../models/domain.models';

export interface FilaAsistenteCarga {
  nombre: string;
  email: string;
  telefono?: string;
  tipoDocumento: string;
  numeroDocumento: string;
}

export interface ResultadoCargaAsistentes {
  cuentasNuevasCreadas: number;
  inscripcionesRegistradas: number;
  filasOmitidasDuplicadoUOtros: number;
  errores: string[];
}

@Injectable({ providedIn: 'root' })
export class InscripcionApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  inscribir(usuarioId: number, eventoId: number): Observable<Inscripcion> {
    return this.http.post<Inscripcion>(this.base + '/api/inscripciones', {
      usuarioId,
      eventoId
    });
  }

  cancelarInscripcion(id: number): Observable<Inscripcion> {
    return this.http.put<Inscripcion>(this.base + '/api/inscripciones/' + id + '/cancelar', null);
  }

  listarPorEvento(eventoId: number): Observable<Inscripcion[]> {
    return this.http.get<Inscripcion[]>(this.base + '/api/inscripciones/evento/' + eventoId);
  }

  listarPorUsuario(usuarioId: number): Observable<Inscripcion[]> {
    return this.http.get<Inscripcion[]>(this.base + '/api/inscripciones/usuario/' + usuarioId);
  }

  checkIn(inscripcionId: number, staffUsuarioId: number): Observable<Inscripcion> {
    return this.http.put<Inscripcion>(
      this.base + '/api/inscripciones/' + inscripcionId + '/check-in',
      { staffUsuarioId }
    );
  }

  checkOut(inscripcionId: number, staffUsuarioId: number): Observable<Inscripcion> {
    return this.http.put<Inscripcion>(
      this.base + '/api/inscripciones/' + inscripcionId + '/check-out',
      { staffUsuarioId }
    );
  }

  checkInQR(codigoQR: string, staffUsuarioId: number, eventoId?: number): Observable<Inscripcion> {
    return this.http.post<Inscripcion>(this.base + '/api/inscripciones/check-in/qr', {
      codigoQR,
      staffUsuarioId,
      eventoId
    });
  }

  checkOutQR(codigoQR: string, staffUsuarioId: number, eventoId?: number): Observable<Inscripcion> {
    return this.http.post<Inscripcion>(this.base + '/api/inscripciones/check-out/qr', {
      codigoQR,
      staffUsuarioId,
      eventoId
    });
  }

  cargaManual(
    eventoId: number,
    organizadorId: number,
    filas: FilaAsistenteCarga[]
  ): Observable<ResultadoCargaAsistentes> {
    return this.http.post<ResultadoCargaAsistentes>(
      this.base + '/api/eventos/' + eventoId + '/asistentes/carga-manual',
      { organizadorId, filas }
    );
  }

  cargaCsv(eventoId: number, organizadorId: number, archivo: File): Observable<ResultadoCargaAsistentes> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    const params = new HttpParams().set('organizadorId', String(organizadorId));
    return this.http.post<ResultadoCargaAsistentes>(
      this.base + '/api/eventos/' + eventoId + '/asistentes/carga-csv',
      formData,
      { params }
    );
  }

  asignarStaff(
    eventoId: number,
    organizadorId: number,
    staffUsuarioId: number,
    funcion: FuncionStaff = 'GENERAL'
  ): Observable<void> {
    return this.http.post<void>(this.base + '/api/eventos/' + eventoId + '/staff/asignacion', {
      organizadorId,
      staffUsuarioId,
      funcion
    });
  }

  cambiarFuncionStaff(
    eventoId: number,
    staffUsuarioId: number,
    organizadorId: number,
    funcion: FuncionStaff
  ): Observable<StaffAsignado> {
    const params = new HttpParams().set('organizadorId', String(organizadorId));
    return this.http.put<StaffAsignado>(
      this.base + '/api/eventos/' + eventoId + '/staff/' + staffUsuarioId + '/funcion',
      { funcion },
      { params }
    );
  }

  desasignarStaff(eventoId: number, staffUsuarioId: number, organizadorId: number): Observable<void> {
    const params = new HttpParams().set('organizadorId', String(organizadorId));
    return this.http.delete<void>(
      this.base + '/api/eventos/' + eventoId + '/staff/' + staffUsuarioId,
      { params }
    );
  }

  staffDelEvento(eventoId: number): Observable<StaffAsignado[]> {
    return this.http.get<StaffAsignado[]>(this.base + '/api/eventos/' + eventoId + '/staff');
  }

  eventosDelStaff(staffUsuarioId: number): Observable<EventoStaff[]> {
    return this.http.get<EventoStaff[]>(this.base + '/api/staff/' + staffUsuarioId + '/eventos');
  }

  asistentesParaStaff(eventoId: number, staffUsuarioId: number, q?: string): Observable<AsistenteEvento[]> {
    let params = new HttpParams().set('staffUsuarioId', String(staffUsuarioId));
    if (q && q.trim().length > 0) params = params.set('q', q.trim());
    return this.http.get<AsistenteEvento[]>(this.base + '/api/eventos/' + eventoId + '/asistentes', {
      params
    });
  }

  asistentesParaOrganizador(eventoId: number, organizadorId: number, q?: string): Observable<AsistenteEvento[]> {
    let params = new HttpParams().set('organizadorId', String(organizadorId));
    if (q && q.trim().length > 0) params = params.set('q', q.trim());
    return this.http.get<AsistenteEvento[]>(
      this.base + '/api/eventos/' + eventoId + '/asistentes/organizador',
      { params }
    );
  }

  aforoEnVivo(eventoId: number): Observable<AforoEnVivo> {
    return this.http.get<AforoEnVivo>(this.base + '/api/eventos/' + eventoId + '/aforo');
  }
}
