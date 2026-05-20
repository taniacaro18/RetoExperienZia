/**
 * Ayuda a saber si un evento sigue "vivo" en el catálogo público.
 * Usa la fecha de fin o inicio + duración, comparado con la hora del navegador.
 */
import { Evento } from '../core/models/domain.models';

// Devuelve en milisegundos cuándo termina el evento (fechaFin o inicio + duración).
export function instanteFinEventoMs(e: Evento): number {
  const inicioMs = new Date(e.fecha).getTime();
  if (e.fechaFin) {
    const finMs = new Date(e.fechaFin).getTime();
    if (finMs >= inicioMs) {
      return finMs;
    }
  }
  const h = e.duracionHoras != null && e.duracionHoras > 0 ? e.duracionHoras : 1;
  return inicioMs + h * 3_600_000;
}

// true si el evento aún no ha pasado su hora de fin (para listarlo en /eventos).
export function eventoSigueVigenteEnCatalogoPublico(e: Evento): boolean {
  return instanteFinEventoMs(e) > Date.now();
}

// true si ya pasó la ventana del evento (ya no debería mostrarse como activo).
export function eventoVentanaYaCerro(e: Evento): boolean {
  return instanteFinEventoMs(e) <= Date.now();
}
