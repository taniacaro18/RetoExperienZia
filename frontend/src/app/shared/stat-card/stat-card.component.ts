import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

export type StatTone = 'brand' | 'success' | 'warn' | 'danger' | 'info';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="ez-stat">
      <div class="flex items-start justify-between gap-3">
        <div class="flex-1 min-w-0">
          <div class="ez-stat-label">{{ label }}</div>
          <div class="ez-stat-value">{{ value }}</div>
          @if (sublabel) {
            <div class="ez-stat-sublabel mt-1">{{ sublabel }}</div>
          }
        </div>
        @if (icon) {
          <div
            class="w-10 h-10 rounded-xl flex items-center justify-center shrink-0"
            [ngClass]="iconClasses()"
          >
            <i class="pi text-lg" [ngClass]="icon"></i>
          </div>
        }
      </div>
    </div>
  `
})
export class StatCardComponent {
  @Input() label = '';
  @Input() value: string | number = 0;
  @Input() sublabel?: string;
  @Input() icon?: string;
  @Input() tone: StatTone = 'brand';

  iconClasses(): string {
    switch (this.tone) {
      case 'success': return 'bg-emerald-100 text-emerald-700';
      case 'warn': return 'bg-coral-100 text-coral-700';
      case 'danger': return 'bg-rose-100 text-rose-700';
      case 'info': return 'bg-sky-100 text-sky-700';
      default: return 'bg-brand-100 text-brand-700';
    }
  }
}
