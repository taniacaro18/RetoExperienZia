import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Pago } from '../models/domain.models';

@Injectable({ providedIn: 'root' })
export class PagoApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl + '/api/pagos';

  /**
   * El organizador sube el comprobante de pago de su evento.
   * El monto se calcula automáticamente en el backend desde el costo del evento.
   */
  registrar(eventoId: number, organizadorId: number, archivo: File): Observable<Pago> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    const params = new HttpParams()
      .set('eventoId', String(eventoId))
      .set('organizadorId', String(organizadorId));
    return this.http.post<Pago>(this.base, formData, { params });
  }

  aprobar(id: number, aprobadorId?: number): Observable<Pago> {
    const params = aprobadorId
      ? new HttpParams().set('aprobadorId', String(aprobadorId))
      : undefined;
    return this.http.put<Pago>(this.base + '/' + id + '/aprobar', null, { params });
  }

  rechazar(id: number, motivo: string, aprobadorId?: number): Observable<Pago> {
    return this.http.put<Pago>(this.base + '/' + id + '/rechazar', {
      motivo,
      aprobadorId
    });
  }

  listarPendientes(): Observable<Pago[]> {
    return this.http.get<Pago[]>(this.base + '/pendientes');
  }

  listarTodos(): Observable<Pago[]> {
    return this.http.get<Pago[]>(this.base);
  }

  listarPorOrganizador(organizadorId: number): Observable<Pago[]> {
    return this.http.get<Pago[]>(this.base + '/organizador/' + organizadorId);
  }
}
