import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

export type LogoSize = 'sm' | 'md' | 'lg' | 'sidebar';

/**
 * Logo oficial ExperienZia (icono + texto con degradado).
 * Sin caja blanca: pensado para fondos violeta del shell y páginas públicas.
 */
@Component({
  selector: 'app-logo',
  standalone: true,
  imports: [RouterLink],
  template: `
    @if (link) {
      <a
        [routerLink]="link"
        (click)="onNavigate?.()"
        class="ez-logo"
        [class.ez-logo--sm]="size === 'sm'"
        [class.ez-logo--md]="size === 'md'"
        [class.ez-logo--lg]="size === 'lg'"
        [class.ez-logo--sidebar]="size === 'sidebar'"
        [attr.aria-label]="ariaLabel"
      >
        <img src="/logo.png" alt="" draggable="false" />
      </a>
    } @else {
      <span
        class="ez-logo"
        [class.ez-logo--sm]="size === 'sm'"
        [class.ez-logo--md]="size === 'md'"
        [class.ez-logo--lg]="size === 'lg'"
        [class.ez-logo--sidebar]="size === 'sidebar'"
        role="img"
        [attr.aria-label]="ariaLabel"
      >
        <img src="/logo.png" alt="" draggable="false" />
      </span>
    }
  `
})
export class LogoComponent {
  @Input() size: LogoSize = 'md';
  @Input() link: string | null = null;
  @Input() ariaLabel = 'ExperienZia — inicio';
  /** Opcional: p. ej. cerrar drawer móvil al pulsar el logo. */
  @Input() onNavigate?: () => void;
}
