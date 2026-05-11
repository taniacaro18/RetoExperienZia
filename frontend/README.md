# ExperienZia · Frontend

Frontend Angular 20 de la plataforma **ExperienZia**, basado en el diseño Figma del proyecto.

## Stack

- **Angular 20** + TypeScript estricto + componentes standalone
- **PrimeNG 20** con preset Aura personalizado (paleta violeta de marca)
- **Tailwind CSS 3** + `tailwindcss-primeui` para coherencia visual con PrimeNG
- **PrimeIcons** + **Lucide** para iconografía
- HTTP `provideHttpClient` con interceptor global de errores → toasts
- Auth con Signals (`AuthStore`) y guards (`authGuard`, `rolGuard`, `noAuthGuard`)
- Layout con header morado degradado y sidebar lateral por rol

## Paleta de marca

Detectada del diseño Figma (`Evento ExperienZia.pdf`):

| Rol             | Tailwind class      | HEX       |
|-----------------|---------------------|-----------|
| Primary         | `bg-brand-600`      | `#7C3AED` |
| Primary hover   | `bg-brand-700`      | `#6D28D9` |
| Primary light   | `bg-brand-50`       | `#F5F3FF` |
| Accent          | `bg-accent-500`     | `#10B981` |
| Surface base    | `bg-surface-50`     | `#F9FAFB` |
| Texto principal | `text-surface-900`  | `#111827` |

Utilidades extra: `bg-brand-gradient` (degradado morado) y `text-brand-gradient`.

## Scripts

```bash
npm install     # instalar dependencias
npm start       # ng serve en http://localhost:4200
npm run build   # build de producción
npm test        # tests con Karma
```

> El frontend espera el backend en `http://localhost:8080` (configurable en `src/environments/environment.ts`).

## Estructura

```
src/
├─ app/
│  ├─ app.config.ts        # Providers globales (PrimeNG + tema, HTTP, router)
│  ├─ app.routes.ts        # Rutas raíz (login + shell con lazy children)
│  ├─ core/
│  │  ├─ models/           # Tipos TypeScript del dominio (espejo del backend)
│  │  ├─ auth/             # AuthStore (Signals), AuthService, guards
│  │  ├─ api/              # Servicios HTTP por dominio (evento, inscripcion, ...)
│  │  └─ interceptors/     # Interceptor global de errores → toast
│  ├─ layout/
│  │  └─ shell/            # Header + sidebar + router-outlet por rol
│  ├─ pages/
│  │  ├─ auth/             # Login, registro, recuperación
│  │  └─ inicio/           # Página inicio temporal
│  └─ theme/
│     └─ experienzia-preset.ts  # Preset Aura PrimeNG con paleta violeta
└─ environments/
   ├─ environment.ts        # apiUrl en dev (localhost:8080)
   └─ environment.prod.ts
```

## Convenciones

- Componentes y páginas son **standalone**.
- Forms con **Reactive Forms** (`FormBuilder.nonNullable.group`).
- Estado con **Signals** y `computed`.
- `*ngIf` y `*ngFor` están reemplazados por `@if` / `@for` (control flow Angular 17+).
- Estilos: prioridad a clases de Tailwind; SCSS solo cuando es necesario.

