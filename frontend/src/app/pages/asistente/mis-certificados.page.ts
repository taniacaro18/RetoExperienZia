import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageService } from 'primeng/api';
import { CertificadoApi } from '../../core/api/certificado.api';
import { AuthStore } from '../../core/auth/auth.store';
import { Certificado } from '../../core/models/domain.models';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-mis-certificados-page',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    ButtonModule,
    TagModule,
    CardModule,
    DialogModule,
    ProgressSpinnerModule
  ],
  templateUrl: './mis-certificados.page.html',
  styleUrl: './mis-certificados.page.scss'
})
export class MisCertificadosPage {
  private readonly api = inject(CertificadoApi);
  private readonly auth = inject(AuthStore);
  private readonly messages = inject(MessageService);

  readonly cargando = signal(true);
  readonly certificados = signal<Certificado[]>([]);
  readonly seleccionado = signal<Certificado | null>(null);

  readonly hayCertificados = computed(() => this.certificados().length > 0);

  /** Igual que el PDF del backend (serial legible en el pie). */
  readonly urlValidacionMostrar = 'experienzia.com/validar';

  ngOnInit() {
    const u = this.auth.usuario();
    if (!u) return;
    this.api.listarPorUsuario(u.id).subscribe({
      next: (lista) => {
        this.certificados.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
  }

  ver(c: Certificado) {
    this.seleccionado.set(c);
  }

  cerrar() {
    this.seleccionado.set(null);
  }

  copiarCodigo(c: Certificado) {
    const codigo = c.codigoUnico || c.codigo || '';
    navigator.clipboard?.writeText(codigo).then(
      () =>
        this.messages.add({
          severity: 'success',
          summary: 'Copiado',
          detail: 'Código del certificado copiado al portapapeles.',
          life: 2500
        }),
      () =>
        this.messages.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudo copiar al portapapeles.'
        })
    );
  }

  /** Abre el PDF oficial generado en servidor (misma plantilla que validación). */
  descargarPDF() {
    const c = this.seleccionado();
    if (!c) return;
    const codigo = (c.codigoUnico || c.codigo || '').trim();
    if (!codigo) {
      this.messages.add({
        severity: 'warn',
        summary: 'Sin código',
        detail: 'Este certificado no tiene código para descargar el PDF.'
      });
      return;
    }
    const url = `${environment.apiUrl}/api/certificados/pdf/${encodeURIComponent(codigo)}`;
    const ventana = window.open(url, '_blank');
    if (!ventana) {
      this.messages.add({
        severity: 'warn',
        summary: 'Bloqueado',
        detail: 'Habilita las ventanas emergentes para descargar el certificado.'
      });
    }
  }

  ciudadExpedicion(c: Certificado): string {
    const s = (c.ciudadExpedicion ?? '').trim();
    return s || 'Bogotá';
  }

  nombreOrganizador(c: Certificado): string {
    const s = (c.nombreOrganizador ?? '').trim();
    return s || 'Organizador';
  }

  fechaEventoTitulada(c: Certificado): string {
    if (!c.fechaEvento) return '—';
    const d = new Date(c.fechaEvento);
    const mes = d.toLocaleString('es-CO', { month: 'long' });
    const mesCap = mes.charAt(0).toUpperCase() + mes.slice(1);
    return `${d.getDate()} de ${mesCap} de ${d.getFullYear()}`;
  }

  fraseLineaRealizado(c: Certificado): string {
    const fecha = this.fechaEventoTitulada(c);
    let s = `Realizado el día ${fecha}`;
    const h = c.duracionHoras;
    if (h != null && h > 0) {
      s += `, con una duración total de ${h} ${h === 1 ? 'hora' : 'horas'}.`;
    } else {
      s += '.';
    }
    return s;
  }

  fraseExpedicion(c: Certificado): string {
    const d = new Date(c.fechaGeneracion);
    const mes = d.toLocaleString('es-CO', { month: 'long' });
    const mesCap = mes.charAt(0).toUpperCase() + mes.slice(1);
    return `a los ${d.getDate()} días del mes de ${mesCap} de ${d.getFullYear()}`;
  }

  serialCertificado(c: Certificado): string {
    const y = new Date(c.fechaGeneracion).getFullYear();
    let alnum = (c.codigoUnico || c.codigo || '').replace(/-/g, '').toUpperCase();
    if (alnum.length > 6) alnum = alnum.slice(0, 6);
    while (alnum.length < 6) alnum += '0';
    return `EXP-${y}-${alnum}`;
  }
}
