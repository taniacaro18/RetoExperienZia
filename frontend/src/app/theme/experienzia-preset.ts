/**
 * Tema visual de PrimeNG para ExperienZia.
 * Partimos del preset Aura y cambiamos el violeta como color primario de la app.
 */
import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

// Exportamos el preset para registrarlo en app.config con providePrimeNG.
export const ExperienziaPreset = definePreset(Aura, {
  semantic: {
    // Escala de violetas (marca ExperienZia).
    primary: {
      50: '#F5F3FF',
      100: '#EDE9FE',
      200: '#DDD6FE',
      300: '#C4B5FD',
      400: '#A78BFA',
      500: '#8B5CF6',
      600: '#7C3AED',
      700: '#6D28D9',
      800: '#5B21B6',
      900: '#4C1D95',
      950: '#2E1065'
    },
    colorScheme: {
      light: {
        // Botones y enlaces principales en modo claro.
        primary: {
          color: '{primary.600}',
          contrastColor: '#ffffff',
          hoverColor: '{primary.700}',
          activeColor: '{primary.800}'
        },
        // Fondos suaves al enfocar campos o filas seleccionadas.
        highlight: {
          background: '{primary.50}',
          focusBackground: '{primary.100}',
          color: '{primary.700}',
          focusColor: '{primary.800}'
        }
      }
    }
  }
});
