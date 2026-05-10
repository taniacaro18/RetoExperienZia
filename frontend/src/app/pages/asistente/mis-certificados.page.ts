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
  templateUrl: './mis-certificados.page.html'
})
export class MisCertificadosPage {
  private readonly api = inject(CertificadoApi);
  private readonly auth = inject(AuthStore);
  private readonly messages = inject(MessageService);

  readonly cargando = signal(true);
  readonly certificados = signal<Certificado[]>([]);
  readonly seleccionado = signal<Certificado | null>(null);

  readonly hayCertificados = computed(() => this.certificados().length > 0);

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

  copiarCodigo(codigo: string) {
    navigator.clipboard?.writeText(codigo).then(
      () => this.messages.add({
        severity: 'success',
        summary: 'Copiado',
        detail: 'Código del certificado copiado al portapapeles.',
        life: 2500
      }),
      () => this.messages.add({
        severity: 'error',
        summary: 'Error',
        detail: 'No se pudo copiar al portapapeles.'
      })
    );
  }

  descargarPDF() {
    const c = this.seleccionado();
    if (!c) return;
    // Versión imprimible mínima (canvas → blob). El backend no genera PDF aún.
    const ventana = window.open('', '_blank');
    if (!ventana) {
      this.messages.add({
        severity: 'warn',
        summary: 'Bloqueado',
        detail: 'Habilita las ventanas emergentes para descargar el certificado.'
      });
      return;
    }
    ventana.document.write(this.htmlImprimible(c));
    ventana.document.close();
    ventana.focus();
    setTimeout(() => ventana.print(), 400);
  }

  private htmlImprimible(c: Certificado): string {
    const fechaEvento = c.fechaEvento ? new Date(c.fechaEvento).toLocaleDateString('es-CO', {
      year: 'numeric', month: 'long', day: '2-digit'
    }) : '';
    const fechaGen = new Date(c.fechaGeneracion).toLocaleDateString('es-CO', {
      year: 'numeric', month: 'long', day: '2-digit'
    });
    return `<!doctype html>
<html><head><meta charset="utf-8"><title>Certificado ${c.codigo}</title>
<style>
  @page { size: A4 landscape; margin: 0; }
  body { font-family: 'Inter', system-ui, sans-serif; margin: 0; padding: 60px;
         background: linear-gradient(135deg, #F5F3FF 0%, #FFFFFF 60%); color: #111827;
         min-height: 100vh; box-sizing: border-box; }
  .marco { border: 14px solid; border-image: linear-gradient(135deg, #8B5CF6, #6D28D9) 1;
           padding: 50px; height: calc(100vh - 120px); display: flex; flex-direction: column;
           justify-content: center; align-items: center; text-align: center; background: white; }
  .titulo { font-size: 14px; letter-spacing: 6px; color: #6D28D9; text-transform: uppercase; margin-bottom: 8px; }
  .marca { font-size: 38px; font-weight: 800; background: linear-gradient(135deg, #8B5CF6, #6D28D9);
           -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin-bottom: 30px; }
  .otorga { font-size: 16px; color: #6B7280; }
  .nombre { font-size: 44px; font-weight: 700; margin: 16px 0 24px 0; color: #111827; }
  .desc { font-size: 18px; color: #374151; max-width: 720px; line-height: 1.5; }
  .evento { font-weight: 600; color: #6D28D9; }
  .footer { margin-top: 40px; display: flex; gap: 80px; align-items: center; }
  .firma { border-top: 2px solid #111827; padding-top: 8px; font-size: 12px; color: #6B7280; min-width: 220px; }
  .codigo { font-family: monospace; font-size: 11px; color: #6B7280; margin-top: 30px; }
</style></head>
<body>
  <div class="marco">
    <div class="titulo">Certificado de Asistencia</div>
    <div class="marca">ExperienZia</div>
    <div class="otorga">Se certifica que</div>
    <div class="nombre">${c.nombreAsistente ?? '—'}</div>
    <div class="desc">
      Asistió y participó en
      <span class="evento">${c.nombreEvento ?? 'el evento'}</span>${fechaEvento ? `, realizado el <strong>${fechaEvento}</strong>` : ''}
      ${c.duracionHoras ? `, con una duración de <strong>${c.duracionHoras} horas</strong>` : ''}.
    </div>
    <div class="footer">
      <div class="firma">Coordinación ExperienZia</div>
      <div class="firma">Fecha de emisión: ${fechaGen}</div>
    </div>
    <div class="codigo">Código de validación: ${c.codigo}</div>
  </div>
  <script>setTimeout(()=>window.print(), 200);</script>
</body></html>`;
  }
}
