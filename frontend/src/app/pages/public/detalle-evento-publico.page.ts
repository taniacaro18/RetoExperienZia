import { Component, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TagModule } from 'primeng/tag';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { EventoApi } from '../../core/api/evento.api';
import { Evento } from '../../core/models/domain.models';
import { eventoEstadoLabel, eventoEstadoSeverity } from '../../shared/estado.helpers';
import { eventoSigueVigenteEnCatalogoPublico } from '../../shared/evento-catalogo.helpers';
import { LogoComponent } from '../../shared/logo/logo.component';
import { VerificarCertificadoModal } from './verificar-certificado.page';

@Component({
  selector: 'app-detalle-evento-publico-page',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    TagModule,
    ProgressSpinnerModule,
    VerificarCertificadoModal,
    LogoComponent
  ],
  templateUrl: './detalle-evento-publico.page.html',
  styleUrl: './detalle-evento-publico.page.scss'
})
export class DetalleEventoPublicoPage {
  private readonly route = inject(ActivatedRoute);
  private readonly eventoApi = inject(EventoApi);

  readonly eventoEstadoLabel = eventoEstadoLabel;
  readonly eventoEstadoSeverity = eventoEstadoSeverity;

  readonly cargando = signal(true);
  readonly evento = signal<Evento | null>(null);
  readonly error = signal<string | null>(null);
  readonly mostrarVerificar = signal(false);

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error.set('ID de evento no válido.');
      this.cargando.set(false);
      return;
    }
    this.eventoApi.obtenerPublico(id).subscribe({
      next: (ev) => {
        if (!eventoSigueVigenteEnCatalogoPublico(ev)) {
          this.error.set('Este evento ya finalizó y no está disponible en el catálogo público.');
          this.evento.set(null);
          this.cargando.set(false);
          return;
        }
        this.evento.set(ev);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se encontró el evento o no está disponible.');
        this.cargando.set(false);
      }
    });
  }
}
