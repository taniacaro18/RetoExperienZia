/**
 * Configuración global de Angular: router, HTTP, PrimeNG e interceptores.
 */
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection
} from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { MessageService, ConfirmationService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { ExperienziaPreset } from './theme/experienzia-preset';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    // Rutas de la app y params como @Input en componentes.
    provideRouter(routes, withComponentInputBinding()),
    // Cliente HTTP con token y manejo de errores.
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
    provideAnimationsAsync(),
    // Tema morado de ExperienZia para PrimeNG.
    providePrimeNG({
      theme: {
        preset: ExperienziaPreset,
        options: {
          prefix: 'p',
          darkModeSelector: '[data-theme="dark"]',
          cssLayer: {
            name: 'primeng',
            order: 'tailwind-base, primeng, tailwind-utilities'
          }
        }
      },
      ripple: true
    }),
    // Servicios para toasts y confirmaciones de PrimeNG.
    MessageService,
    ConfirmationService
  ]
};
