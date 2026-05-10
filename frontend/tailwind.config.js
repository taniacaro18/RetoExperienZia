/** @type {import('tailwindcss').Config} */
const primeui = require('tailwindcss-primeui');

module.exports = {
  content: ['./src/**/*.{html,ts}'],
  darkMode: ['selector', '[data-theme="dark"]'],
  theme: {
    extend: {
      // Paleta ExperienZia: violeta/lila dominante (Figma) con detalles del logo.
      colors: {
        brand: {
          50: '#F4EEFB',
          100: '#E8DDF6',
          200: '#D2BCEB',
          300: '#BC9CDF',
          400: '#A77FD3', // violeta lila body bg
          500: '#9061C2', // violeta header/sidebar
          600: '#7C49AE', // violeta botones
          700: '#65378F',
          800: '#4E2A6F',
          900: '#372050'
        },
        coral: {
          50: '#FFF4F1',
          100: '#FFE2D9',
          200: '#FFC5B3',
          300: '#FFA48C',
          400: '#FB8568',
          500: '#EF6C4D',
          600: '#D8553A',
          700: '#A8412C'
        },
        accent: {
          50: '#ECFDF5',
          100: '#D1FAE5',
          200: '#A7F3D0',
          300: '#6EE7B7',
          400: '#34D399',
          500: '#10B981',
          600: '#059669',
          700: '#047857'
        },
        surface: {
          0: '#FFFFFF',
          50: '#FAF8FB',
          100: '#F3F0F6',
          200: '#E6E0EC',
          300: '#CFC6D8',
          400: '#9C93A8',
          500: '#6B6377',
          700: '#3B3445',
          900: '#1B1726'
        }
      },
      fontFamily: {
        sans: ['"Inter"', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif']
      },
      boxShadow: {
        card: '0 1px 2px rgba(60, 40, 90, 0.04), 0 6px 16px rgba(120, 96, 168, 0.10)',
        soft: '0 2px 6px rgba(60, 40, 90, 0.05)',
        violet: '0 8px 24px rgba(101, 55, 143, 0.18)'
      },
      backgroundImage: {
        'brand-gradient': 'linear-gradient(135deg, #C7B9E2 0%, #E8B0B0 50%, #F4C2A8 100%)',
        'brand-gradient-strong': 'linear-gradient(135deg, #9061C2 0%, #7C49AE 100%)'
      }
    }
  },
  plugins: [primeui]
};
