/**
 * Origen de retorno desde el detalle de evento (`/eventos/:id`).
 * Solo se aceptan claves de esta lista (evita open redirect por query arbitraria).
 */
export const RETORNO_EVENTO_DETALLE: Record<string, { path: string; label: string }> = {
  'mis-inscripciones': { path: '/mis-inscripciones', label: 'Volver a mis inscripciones' },
  eventos: { path: '/eventos', label: 'Volver al catálogo' },
  inicio: { path: '/inicio', label: 'Volver al inicio' },
  'organizador-eventos': { path: '/organizador/eventos', label: 'Volver a mis eventos' },
  'admin-eventos': { path: '/admin/eventos', label: 'Volver a administración de eventos' }
};

export function destinoRetornoEventoDetalle(
  key: string | null | undefined
): { path: string; label: string } | null {
  if (!key) return null;
  return RETORNO_EVENTO_DETALLE[key] ?? null;
}
