import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Pago } from '../models/domain.models';
import { environment } from '../../../environments/environment';

export function urlComprobantePago(p: Pago | null | undefined): string {
  if (!p?.comprobanteUrl) return '';
  const u = p.comprobanteUrl;
  if (u.startsWith('http')) return u;
  if (u.startsWith('/')) return environment.apiUrl + u;
  return environment.apiUrl + '/' + u;
}

export function urlComprobanteSeguraPago(
  p: Pago | null | undefined,
  sanitizer: DomSanitizer
): SafeResourceUrl {
  return sanitizer.bypassSecurityTrustResourceUrl(urlComprobantePago(p));
}

export function esComprobanteImagen(p: Pago | null | undefined): boolean {
  if (!p?.comprobanteUrl) return false;
  return /\.(jpe?g|png|gif|webp|bmp)(\?|$)/i.test(p.comprobanteUrl);
}

export function esComprobantePdf(p: Pago | null | undefined): boolean {
  return !!p?.comprobanteUrl && p.comprobanteUrl.toLowerCase().includes('.pdf');
}
