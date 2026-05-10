import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

/**
 * Logo oficial de ExperienZia.
 * - mode="full": logo + nombre.
 * - mode="icon": solo la estrella.
 * - size en px (alto del logo).
 */
@Component({
  selector: 'app-logo',
  standalone: true,
  imports: [CommonModule],
  template: `
    <a [class]="containerClass" [attr.aria-label]="'ExperienZia'">
      <img
        src="/logo.png"
        alt="ExperienZia"
        [style.height.px]="size"
        class="object-contain select-none"
        draggable="false"
      />
    </a>
  `
})
export class LogoComponent {
  @Input() size = 40;
  @Input() containerClass = 'inline-flex items-center';
}
