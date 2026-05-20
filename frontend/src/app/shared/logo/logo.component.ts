/**
 * Muestra el logo de ExperienZia (imagen en /logo.png).
 * Puede ser solo decorativo o un enlace con routerLink; sirve en el shell y páginas públicas.
 */
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

// Tamaños predefinidos del logo según dónde se use.
export type LogoSize = 'sm' | 'md' | 'lg' | 'sidebar';

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
  // Ruta interna; si es null el logo no es clickeable.
  @Input() link: string | null = null;
  // Texto para lectores de pantalla.
  @Input() ariaLabel = 'ExperienZia — inicio';
  // Callback opcional al hacer clic (ej. cerrar el menú móvil).
  @Input() onNavigate?: () => void;
}
