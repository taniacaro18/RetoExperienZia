import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { InputTextModule } from 'primeng/inputtext';
import { EventoApi } from '../../core/api/evento.api';
import { Evento } from '../../core/models/domain.models';
import { eventoSigueVigenteEnCatalogoPublico } from '../../shared/evento-catalogo.helpers';
import { LogoComponent } from '../../shared/logo/logo.component';
import { VerificarCertificadoModal } from './verificar-certificado.page';

@Component({
  selector: 'app-catalogo-publico-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    RouterLink,
    ProgressSpinnerModule,
    InputTextModule,
    VerificarCertificadoModal,
    LogoComponent
  ],
  templateUrl: './catalogo-publico.page.html',
  styleUrl: './catalogo-publico.page.scss'
})
export class CatalogoPublicoPage {
  private readonly eventoApi = inject(EventoApi);

  readonly cargando = signal(true);
  readonly todos = signal<Evento[]>([]);

  readonly nombre = signal('');
  readonly categoria = signal('');
  readonly mostrarVerificar = signal(false);

  readonly filtrados = computed(() => {
    let list = this.todos().filter(eventoSigueVigenteEnCatalogoPublico);
    const n = this.nombre().trim().toLowerCase();
    const cat = this.categoria().trim().toLowerCase();
    if (n) {
      list = list.filter((e) => (e.nombre || '').toLowerCase().includes(n));
    }
    if (cat) {
      list = list.filter((e) => (e.categoria || '').toLowerCase().includes(cat));
    }
    return [...list].sort((a, b) => new Date(a.fecha).getTime() - new Date(b.fecha).getTime());
  });

  ngOnInit() {
    this.cargando.set(true);
    this.eventoApi.catalogoPublicos().subscribe({
      next: (lista) => {
        this.todos.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
  }
}
