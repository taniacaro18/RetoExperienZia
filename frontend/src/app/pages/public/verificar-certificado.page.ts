import { Component, inject, signal, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { CertificadoApi } from '../../core/api/certificado.api';
import { Certificado } from '../../core/models/domain.models';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-verificar-certificado-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './verificar-certificado.page.html',
  styleUrl: './verificar-certificado.page.scss'
})
export class VerificarCertificadoModal {
  private readonly certApi = inject(CertificadoApi);
  private readonly messages = inject(MessageService);

  readonly cerrarModal = output<void>();

  readonly codigo = signal('');
  readonly cargando = signal(false);
  readonly resultado = signal<Certificado | null>(null);

  buscar() {
    const c = this.codigo().trim();
    if (!c) {
      this.messages.add({
        severity: 'warn',
        summary: 'Código requerido',
        detail: 'Ingresa el código impreso en el certificado.',
        life: 4000
      });
      return;
    }
    this.cargando.set(true);
    this.resultado.set(null);
    this.certApi.validar(encodeURIComponent(c.trim())).subscribe({
      next: (cert) => {
        this.cargando.set(false);
        this.resultado.set(cert);
      },
      error: () => {
        this.cargando.set(false);
        this.resultado.set(null);
        this.messages.add({
          severity: 'error',
          summary: 'No verificado',
          detail: 'El código no existe o no es válido.',
          life: 5000
        });
      }
    });
  }

  descargarPdf() {
    const cert = this.resultado();
    const codigoRaw = cert?.codigoUnico || cert?.codigo;
    if (!codigoRaw) return;
    const url =
      `${environment.apiUrl}/api/certificados/pdf/` + encodeURIComponent(codigoRaw.trim());
    window.open(url, '_blank', 'noopener,noreferrer');
  }

  cerrar() {
    this.cerrarModal.emit();
  }
}
