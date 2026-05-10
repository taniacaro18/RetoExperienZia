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
  readonly actual = signal(0);
  readonly maximo = signal(0);

  @Input() set value(v: number) { this.actual.set(v ?? 0); }
  @Input() set max(v: number) { this.maximo.set(v ?? 0); }
  @Input() height = 8;
  @Input() leftLabel = 'Aforo';
  @Input() hideLabel = false;
  @Input() containerClass = '';

  readonly porcentaje = computed(() => porcentajeOcupacion(this.actual(), this.maximo()));
  readonly color = computed(() => colorOcupacion(this.porcentaje()));
}
