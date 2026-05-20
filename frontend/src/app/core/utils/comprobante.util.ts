/**
 * Utilidades para mostrar la URL del comprobante de pago (imagen o PDF).
 */
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Pago } from '../models/domain.models';
import { environment } from '../../../environments/environment';

// Arma la URL completa del archivo del comprobante.
export function urlComprobantePago(p: Pago | null | undefined): string {
  if (!p?.comprobanteUrl) return '';
  const u = p.comprobanteUrl;
  if (u.startsWith('http')) return u;
  if (u.startsWith('/')) return environment.apiUrl + u;
  return environment.apiUrl + '/' + u;
}

// Igual pero segura para usar en iframe/img de Angular.
export function urlComprobanteSeguraPago(
  p: Pago | null | undefined,
  sanitizer: DomSanitizer
): SafeResourceUrl {
  return sanitizer.bypassSecurityTrustResourceUrl(urlComprobantePago(p));
}

// True si el archivo parece imagen (jpg, png, etc.).
export function esComprobanteImagen(p: Pago | null | undefined): boolean {
  if (!p?.comprobanteUrl) return false;
  return /\.(jpe?g|png|gif|webp|bmp)(\?|$)/i.test(p.comprobanteUrl);
}

// True si el archivo es PDF.
export function esComprobantePdf(p: Pago | null | undefined): boolean {
  return !!p?.comprobanteUrl && p.comprobanteUrl.toLowerCase().includes('.pdf');
}
