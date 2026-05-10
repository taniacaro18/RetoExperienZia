import { CommonModule } from '@angular/common';
import { Component, Input, computed, signal } from '@angular/core';

interface DonutSegmento { label: string; valor: number; color: string; }

@Component({
  selector: 'app-donut',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex items-center gap-4">
      <svg [attr.width]="size" [attr.height]="size" [attr.viewBox]="'0 0 ' + size + ' ' + size" class="shrink-0">
        @if (totalSig() === 0) {
          <circle [attr.cx]="cx" [attr.cy]="cy" [attr.r]="radio" fill="none" stroke="#E6E0EC" [attr.stroke-width]="grosor" />
        } @else {
          @for (seg of segmentos(); track seg.label) {
            <circle
              [attr.cx]="cx"
              [attr.cy]="cy"
              [attr.r]="radio"
              fill="none"
              [attr.stroke]="seg.color"
              [attr.stroke-width]="grosor"
              [attr.stroke-dasharray]="seg.dash"
              [attr.stroke-dashoffset]="seg.offset"
              [attr.transform]="'rotate(-90 ' + cx + ' ' + cy + ')'"
              stroke-linecap="butt"
            />
          }
        }
        <text [attr.x]="cx" [attr.y]="cy - 4" text-anchor="middle" class="font-bold fill-surface-900" style="font-size: 18px;">
          {{ porcentajePrincipal() }}%
        </text>
        <text [attr.x]="cx" [attr.y]="cy + 14" text-anchor="middle" class="fill-surface-500" style="font-size: 10px;">
          {{ leyendaCentro }}
        </text>
      </svg>
      <div class="space-y-1.5 text-xs">
        @for (seg of segmentos(); track seg.label) {
          <div class="flex items-center gap-2">
            <span class="w-3 h-3 rounded" [style.background-color]="seg.color"></span>
            <span class="text-surface-700">{{ seg.label }}</span>
            <span class="text-surface-900 font-semibold ml-auto">{{ seg.valor }}</span>
          </div>
        }
      </div>
    </div>
  `
})
export class DonutComponent {
  readonly size = 128;
  readonly grosor = 14;
  readonly cx = this.size / 2;
  readonly cy = this.size / 2;
  readonly radio = (this.size - this.grosor) / 2;
  readonly circunferencia = 2 * Math.PI * this.radio;

  @Input() leyendaCentro = '';

  private readonly datos = signal<DonutSegmento[]>([]);
  @Input() set data(v: DonutSegmento[]) { this.datos.set(v ?? []); }

  readonly totalSig = computed(() => this.datos().reduce((a, b) => a + b.valor, 0));

  readonly segmentos = computed(() => {
    const total = this.totalSig();
    if (total === 0) return [];
    let acumulado = 0;
    return this.datos().map((seg) => {
      const fraccion = seg.valor / total;
      const dashLen = fraccion * this.circunferencia;
      const segmento = {
        ...seg,
        dash: `${dashLen} ${this.circunferencia - dashLen}`,
        offset: -acumulado
      };
      acumulado += dashLen;
      return segmento;
    });
  });

  readonly porcentajePrincipal = computed(() => {
    const total = this.totalSig();
    if (total === 0 || this.datos().length === 0) return 0;
    return Math.round((this.datos()[0].valor / total) * 100);
  });
}
