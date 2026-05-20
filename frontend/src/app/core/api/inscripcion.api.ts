/**
 * Llama al backend de inscripciones, check-in/out, staff y aforo.
 */
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_GLOBAL_TOAST } from '../interceptors/error.interceptor';
import {
  AforoEnVivo,
  AsistenteEvento,
  EventoStaff,
  FuncionStaff,
  Inscripcion,
  StaffAsignado
} from '../models/domain.models';

// Una fila del Excel/CSV de carga masiva de asistentes.
export interface FilaAsistenteCarga {
  nombre: string;
  email: string;
  telefono?: string;
  tipoDocumento: string;
  numeroDocumento: string;
}

// Resumen después de subir asistentes manual o por CSV.
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

  // El asistente se inscribe a un evento.
  inscribir(usuarioId: number, eventoId: number): Observable<Inscripcion> {
    return this.http.post<Inscripcion>(this.base + '/api/inscripciones', {
      usuarioId,
      eventoId
    });
  }

  // Cancela la inscripción del asistente.
  cancelarInscripcion(id: number): Observable<Inscripcion> {
    return this.http.put<Inscripcion>(this.base + '/api/inscripciones/' + id + '/cancelar', null);
  }

  // Inscritos de un evento (organizador/staff).
  listarPorEvento(eventoId: number): Observable<Inscripcion[]> {
    return this.http.get<Inscripcion[]>(this.base + '/api/inscripciones/evento/' + eventoId);
  }

  // Mis inscripciones como asistente.
  listarPorUsuario(usuarioId: number): Observable<Inscripcion[]> {
    return this.http.get<Inscripcion[]>(this.base + '/api/inscripciones/usuario/' + usuarioId);
  }

  // Staff marca entrada manual por id de inscripción.
  checkIn(inscripcionId: number, staffUsuarioId: number): Observable<Inscripcion> {
    return this.http.put<Inscripcion>(
      this.base + '/api/inscripciones/' + inscripcionId + '/check-in',
      { staffUsuarioId },
      { headers: new HttpHeaders().set(SKIP_GLOBAL_TOAST, '1') }
    );
  }

  // Staff marca salida manual.
  checkOut(inscripcionId: number, staffUsuarioId: number): Observable<Inscripcion> {
    return this.http.put<Inscripcion>(
      this.base + '/api/inscripciones/' + inscripcionId + '/check-out',
      { staffUsuarioId },
      { headers: new HttpHeaders().set(SKIP_GLOBAL_TOAST, '1') }
    );
  }

  // Entrada leyendo código QR.
  checkInQR(codigoQR: string, staffUsuarioId: number, eventoId?: number): Observable<Inscripcion> {
    return this.http.post<Inscripcion>(this.base + '/api/inscripciones/check-in/qr', {
      codigoQR,
      staffUsuarioId,
      eventoId
    }, { headers: new HttpHeaders().set(SKIP_GLOBAL_TOAST, '1') });
  }

  // Salida leyendo código QR.
  checkOutQR(codigoQR: string, staffUsuarioId: number, eventoId?: number): Observable<Inscripcion> {
    return this.http.post<Inscripcion>(this.base + '/api/inscripciones/check-out/qr', {
      codigoQR,
      staffUsuarioId,
      eventoId
    }, { headers: new HttpHeaders().set(SKIP_GLOBAL_TOAST, '1') });
  }

  // Organizador pega filas de asistentes en pantalla.
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

  // Organizador sube archivo CSV de asistentes.
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

  // Asigna un staff a un evento con una función.
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

  // Cambia la función del staff en el evento.
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

  // Quita al staff del evento.
  desasignarStaff(eventoId: number, staffUsuarioId: number, organizadorId: number): Observable<void> {
    const params = new HttpParams().set('organizadorId', String(organizadorId));
    return this.http.delete<void>(
      this.base + '/api/eventos/' + eventoId + '/staff/' + staffUsuarioId,
      { params }
    );
  }

  // Lista staff asignado a un evento.
  staffDelEvento(eventoId: number): Observable<StaffAsignado[]> {
    return this.http.get<StaffAsignado[]>(this.base + '/api/eventos/' + eventoId + '/staff');
  }

  // Eventos donde trabaja un staff.
  eventosDelStaff(staffUsuarioId: number): Observable<EventoStaff[]> {
    return this.http.get<EventoStaff[]>(this.base + '/api/staff/' + staffUsuarioId + '/eventos');
  }

  // Lista asistentes para el staff (con búsqueda opcional).
  asistentesParaStaff(eventoId: number, staffUsuarioId: number, q?: string): Observable<AsistenteEvento[]> {
    let params = new HttpParams().set('staffUsuarioId', String(staffUsuarioId));
    if (q && q.trim().length > 0) params = params.set('q', q.trim());
    return this.http.get<AsistenteEvento[]>(this.base + '/api/eventos/' + eventoId + '/asistentes', {
      params
    });
  }

  // Lista asistentes para el organizador del evento.
  asistentesParaOrganizador(eventoId: number, organizadorId: number, q?: string): Observable<AsistenteEvento[]> {
    let params = new HttpParams().set('organizadorId', String(organizadorId));
    if (q && q.trim().length > 0) params = params.set('q', q.trim());
    return this.http.get<AsistenteEvento[]>(
      this.base + '/api/eventos/' + eventoId + '/asistentes/organizador',
      { params }
    );
  }

  // Contadores de aforo en tiempo real.
  aforoEnVivo(eventoId: number): Observable<AforoEnVivo> {
    return this.http.get<AforoEnVivo>(this.base + '/api/eventos/' + eventoId + '/aforo');
  }
}
