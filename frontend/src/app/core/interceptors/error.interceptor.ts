import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MessageService } from 'primeng/api';
import { catchError, throwError } from 'rxjs';

/**
 * Interceptor global: traduce errores HTTP a toasts y deja pasar la excepción
 * para que cada componente decida si reacciona.
 */
export const SKIP_GLOBAL_TOAST = 'X-Skip-Global-Toast';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const messageService = inject(MessageService, { optional: true });
  const skipToast = req.headers.has(SKIP_GLOBAL_TOAST);
  const cleanReq = skipToast ? req.clone({ headers: req.headers.delete(SKIP_GLOBAL_TOAST) }) : req;

  return next(cleanReq).pipe(
    catchError((err: HttpErrorResponse) => {
      if (!skipToast) {
        const mensaje = extraerMensaje(err);
        if (messageService) {
          messageService.add({
            severity: severidadPorEstado(err.status),
            summary: tituloPorEstado(err.status),
            detail: mensaje,
            life: 4500
          });
        } else {
          console.error('[HTTP]', err.status, mensaje);
        }
      }
      return throwError(() => err);
    })
  );
};

function extraerMensaje(err: HttpErrorResponse): string {
  if (err.error && typeof err.error === 'object' && 'message' in err.error) {
    return String(err.error.message);
  }
  if (typeof err.error === 'string' && err.error.trim().length > 0) {
    return err.error;
  }
  if (err.message) return err.message;
  return 'Error inesperado al procesar la solicitud.';
}

function severidadPorEstado(status: number): 'error' | 'warn' | 'info' {
  if (status === 0) return 'error';
  if (status >= 500) return 'error';
  if (status === 401 || status === 403) return 'warn';
  if (status >= 400) return 'warn';
  return 'info';
}

function tituloPorEstado(status: number): string {
  switch (status) {
    case 0: return 'Sin conexión con el servidor';
    case 400: return 'Datos inválidos';
    case 401: return 'No autorizado';
    case 403: return 'Acceso denegado';
    case 404: return 'No encontrado';
    case 409: return 'Conflicto';
    case 500: return 'Error del servidor';
    default: return 'Error';
  }
}
