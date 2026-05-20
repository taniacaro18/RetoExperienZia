/**
 * Barra de progreso que muestra cuántas personas hay respecto al aforo máximo.
 * El color cambia según el porcentaje (helpers en estado.helpers).
 */
import { CommonModule } from '@angular/common';
import { Component, Input, computed, signal } from '@angular/core';
import { colorOcupacion, porcentajeOcupacion } from '../estado.helpers';

@Component({
  selector: 'app-aforo-bar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div [class]="containerClass">
      <div class="flex items-center justify-between text-xs mb-1" [class.hidden]="hideLabel">
        <span class="text-surface-500">{{ leftLabel }}</span>
        <span class="text-surface-700 font-semibold">{{ actual() }}/{{ maximo() }} · {{ porcentaje() }}%</span>
      </div>
      <div class="bg-surface-100 rounded-full overflow-hidden" [style.height.px]="height">
        <div class="h-full transition-all" [class]="color()" [style.width.%]="porcentaje()"></div>
      </div>
    </div>
  `
})
export class AforoBarComponent {
  // Personas actuales (ocupación).
  readonly actual = signal(0);
  // Capacidad máxima del salón o evento.
  readonly maximo = signal(0);

  @Input() set value(v: number) { this.actual.set(v ?? 0); }
  @Input() set max(v: number) { this.maximo.set(v ?? 0); }
  // Altura de la barra en píxeles.
  @Input() height = 8;
  // Texto a la izquierda, por defecto "Aforo".
  @Input() leftLabel = 'Aforo';
  // Si true, no muestra la fila de etiquetas arriba.
  @Input() hideLabel = false;
  // Clases extra para el contenedor (margen, ancho, etc.).
  @Input() containerClass = '';

  // Porcentaje redondeado entre 0 y 100.
  readonly porcentaje = computed(() => porcentajeOcupacion(this.actual(), this.maximo()));
  // Clase de color de Tailwind según qué tan llena está la barra.
  readonly color = computed(() => colorOcupacion(this.porcentaje()));
}
