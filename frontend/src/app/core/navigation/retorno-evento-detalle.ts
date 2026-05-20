/**
 * Rutas permitidas para el botón "Volver" en el detalle de un evento (/eventos/:id).
 * Solo aceptamos claves de esta lista para no redirigir a URLs raras por query string.
 */
export const RETORNO_EVENTO_DETALLE: Record<string, { path: string; label: string }> = {
  'mis-inscripciones': { path: '/mis-inscripciones', label: 'Volver a mis inscripciones' },
  eventos: { path: '/eventos', label: 'Volver al catálogo' },
  inicio: { path: '/inicio', label: 'Volver al inicio' },
  'organizador-eventos': { path: '/organizador/eventos', label: 'Volver a mis eventos' },
  'admin-eventos': { path: '/admin/eventos', label: 'Volver a administración de eventos' }
};

// Busca la clave ?from= en la URL y devuelve path + texto del botón, o null si no existe.
export function destinoRetornoEventoDetalle(
  key: string | null | undefined
): { path: string; label: string } | null {
  if (!key) return null;
  return RETORNO_EVENTO_DETALLE[key] ?? null;
}
