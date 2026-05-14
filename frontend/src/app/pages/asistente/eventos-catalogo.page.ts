import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { DatePickerModule } from 'primeng/datepicker';
import { TagModule } from 'primeng/tag';
import { CardModule } from 'primeng/card';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { EventoApi, EventoSearchCriteria } from '../../core/api/evento.api';
import { Evento } from '../../core/models/domain.models';

@Component({
  selector: 'app-eventos-catalogo-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DatePipe,
    ButtonModule,
    InputTextModule,
    SelectModule,
    DatePickerModule,
    TagModule,
    CardModule,
    ProgressSpinnerModule
  ],
  templateUrl: './eventos-catalogo.page.html'
})
export class EventosCatalogoPage {
  private readonly api = inject(EventoApi);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly cargando = signal(true);
  readonly eventos = signal<Evento[]>([]);

  readonly filtros = this.fb.group({
    nombre: [''],
    categoria: [''],
    tipoEvento: ['' as 'PUBLICO' | 'PRIVADO' | ''],
    fechaDesde: [null as Date | null],
    fechaHasta: [null as Date | null]
  });

  readonly tiposEvento = [
    { label: 'Todos', value: '' },
    { label: 'Públicos', value: 'PUBLICO' },
    { label: 'Privados', value: 'PRIVADO' }
  ];

  readonly eventosFiltrados = computed(() => this.eventos());

  ngOnInit() {
    // Si el header pasó un parámetro `q`, lo usamos como filtro inicial de nombre.
    this.route.queryParamMap.subscribe((params) => {
      const q = params.get('q')?.trim();
      if (q) {
        this.filtros.patchValue({ nombre: q });
      }
      this.buscar();
    });
  }

  buscar() {
    this.cargando.set(true);
    const v = this.filtros.value;
    const criterios: EventoSearchCriteria = {
      nombre: v.nombre || undefined,
      categoria: v.categoria || undefined,
      tipoEvento: (v.tipoEvento || undefined) as any,
      estado: 'ACTIVO',
      fechaDesde: v.fechaDesde ? this.toIsoString(v.fechaDesde) : undefined,
      fechaHasta: v.fechaHasta ? this.toIsoString(v.fechaHasta) : undefined
    };
    this.api.buscar(criterios).subscribe({
      next: (lista) => {
        // Por seguridad, solo mostramos públicos al asistente.
        this.eventos.set(lista.filter((e) => e.tipoEvento === 'PUBLICO'));
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
  }

  limpiar() {
    this.filtros.reset({
      nombre: '',
      categoria: '',
      tipoEvento: '',
      fechaDesde: null,
      fechaHasta: null
    });
    this.buscar();
  }

  abrir(e: Evento) {
    this.router.navigate(['/eventos', e.id], { queryParams: { retorno: 'eventos' } });
  }

  cuposDisponibles(e: Evento): number {
    return Math.max(0, e.aforoMaximo - e.aforoActual);
  }

  porcentajeOcupacion(e: Evento): number {
    if (!e.aforoMaximo) return 0;
    return Math.min(100, Math.round((e.aforoActual / e.aforoMaximo) * 100));
  }

  private toIsoString(d: Date): string {
    return new Date(d.getFullYear(), d.getMonth(), d.getDate(), 0, 0, 0).toISOString();
  }
}
