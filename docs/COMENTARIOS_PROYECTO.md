# Guía de comentarios del proyecto ExperienZia

Este documento resume cómo está documentado el código para entregar o estudiar el proyecto.

## Estilo (como estudiante principiante)

- **Español** sencillo, sin tecnicismos innecesarios.
- Bloque `/** ... */` al inicio de cada clase, componente o enum: qué es y para qué sirve.
- Comentarios `//` antes de campos, métodos y bloques importantes.
- En relaciones de base de datos: se explica que `@ManyToOne` es la “llave foránea” hacia otra tabla.

## Qué está comentado

| Capa | Ubicación | Archivos aprox. |
|------|-----------|-----------------|
| Backend – entrada | `Application.java` | 1 |
| Backend – entidades y enums | `entity/` | 19 |
| Backend – controladores REST | `controller/` | 11 |
| Backend – servicios e implementaciones | `service/`, `impl/` | 19 |
| Backend – repositorios | `repository/` | 9 |
| Backend – DTOs | `dto/` | 39 |
| Backend – configuración y seguridad | `config/`, `security/` | 9 |
| Backend – utilidades, specs, excepciones | `util/`, `spec/`, `exceptions/` | 6 |
| Frontend – núcleo | `core/` (api, auth, models, interceptors…) | 21 |
| Frontend – rutas y app | `app.ts`, `app.config.ts`, `app.routes.ts` | 3 |
| Frontend – pantallas | `pages/**/*.ts` | 31 |
| Frontend – componentes compartidos | `shared/`, `layout/` | 10 |

**Total aproximado: 170+ archivos `.java` y `.ts` con comentarios.**

## Qué no se comentó (a propósito)

- Plantillas **`.html`**: la vista ya se entiende leyendo el `.ts` del mismo componente.
- Archivos de **prueba** (`*Test.java`, `app.spec.ts`) salvo que se agreguen después.
- **`application.properties`** y **`environment.ts`**: configuración; conviene documentarlos en el README de despliegue.

## Cómo leer el proyecto

1. Empieza por `Application.java` y `app.routes.ts` (mapa general).
2. Revisa `domain.models.ts` (tipos que usa Angular).
3. Sigue un flujo completo, por ejemplo: `evento-form.page.ts` → `evento.api.ts` → `EventoController.java` → `EventoServiceImpl.java` → `Evento.java`.

## Mantener los comentarios

Al agregar código nuevo, copia el mismo estilo: una frase clara de **qué hace** y **quién lo usa** (admin, organizador, asistente, staff).
