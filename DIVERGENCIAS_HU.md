# Divergencias entre HUs originales y la implementación actual

Este documento resume **qué se cambió, qué se añadió y qué decisiones tomadas
en sesión** respecto al PDF original de Historias de Usuario y los diagramas de
clases / casos de uso. Es la base para que las HUs queden actualizadas y
coherentes con el sistema entregado.

> Convención: `[+]` añadido, `[~]` modificado respecto a la HU original,
> `[-]` retirado del alcance, `[!]` regla extra que conviene documentar.

---

## 1. Roles y autenticación

### 1.1 Registro de usuarios
- `[~]` Solo se permite auto-registro como **ASISTENTE** o **ORGANIZADOR**.
  ADMIN y STAFF nunca llegan por la pantalla pública de registro.
- `[+]` El **ORGANIZADOR** queda en estado `PENDIENTE` hasta que un ADMIN lo
  apruebe. Mientras tanto no puede iniciar sesión (HTTP 403 con mensaje claro).
- `[+]` Se devuelve mensaje específico cuando el correo, número de documento o
  teléfono están duplicados. El frontend muestra ese detalle en el toast.

### 1.2 Recuperar contraseña
- `[~]` Se renombra "envío de correo" por **generación de contraseña temporal
  in-app**. El sistema valida `email + número de documento` y devuelve una
  contraseña aleatoria que el usuario debe cambiar desde su perfil.
- `[+]` Estado `INACTIVO` o `PENDIENTE` impide recuperar contraseña.

### 1.3 Edición de perfil
- `[+]` Cualquier rol puede editar nombre, email, teléfono, tipo y número de
  documento, y cambiar la contraseña actual.
- `[+]` Validaciones de unicidad iguales al registro.

### 1.4 Cuentas STAFF
- `[+]` Solo el ORGANIZADOR puede crear cuentas STAFF (auto-registro está
  bloqueado).
- `[+]` La contraseña inicial de un staff creado por organizador es **su
  número de documento**.
- `[+]` El ORGANIZADOR puede activar/desactivar a su propio staff.

### 1.5 Reactivación
- `[+]` ADMIN puede reactivar usuarios `INACTIVO` (HU original solo contempla
  desactivación).

---

## 2. Eventos

### 2.1 Creación de evento
- `[~]` La **duración en horas** y el **costo** ya **no son campos del
  formulario**: se calculan automáticamente.
  - `duracionHoras = (fechaFin - fechaInicio)` en horas (mínimo 1).
  - `costo = duracionHoras * 100.000 COP` (tarifa de plataforma fija por hora).
- `[~]` `ubicacion` es opcional; si se deja vacío se asigna **"Salón
  principal"** por defecto.
- `[!]` `aforoMaximo` tiene un tope físico de **600 personas**.
- `[+]` Se exige `fechaInicio` y `fechaFin`; `fechaFin > fechaInicio`.

### 2.2 Flujo de aprobación
HU original: el evento creado pasa a "publicado". Implementado:
1. ORGANIZADOR crea el evento → estado `PENDIENTE`.
2. ADMIN aprueba preliminarmente → estado `APROBADO`.
3. ORGANIZADOR sube **comprobante de pago** (imagen o PDF, ≤10 MB).
4. ADMIN valida el comprobante:
   - Aprobado → evento queda `ACTIVO` y aparece en catálogo.
   - Rechazado → ORGANIZADOR debe volver a subir un nuevo comprobante.
- `[+]` Un evento `RECHAZADO` se puede editar y volver a enviar (vuelve a
  `PENDIENTE`).
- `[+]` ORGANIZADOR puede cancelar su evento con motivo (notifica a inscritos).

### 2.3 Modelo de pago
- `[~]` **Solo el ORGANIZADOR paga**. Asistente y Staff no aparecen en el
  módulo de pagos.
- `[~]` El asistente **no ve costos** en ninguna parte del sistema (catálogo,
  detalle de evento, certificado).

### 2.4 Inscripción y aforo
- `[+]` El ORGANIZADOR queda **auto-inscrito** como asistente principal de su
  evento (no aparece el botón "Inscribirse" sobre eventos propios).
- `[+]` ORGANIZADOR y STAFF también pueden inscribirse a eventos públicos de
  terceros, como cualquier asistente.
- `[+]` El sistema valida cupo disponible en cada inscripción y devuelve error
  específico si está lleno.

---

## 3. Asistentes

### 3.1 Catálogo (asistente)
- `[+]` Filtros por nombre, categoría, tipo (público/privado), rango de fechas.
- `[+]` Solo se muestran eventos `ACTIVO` y `PUBLICO`.
- `[+]` La búsqueda del header del shell se conecta a este catálogo (parámetro
  `q`).

### 3.2 Carga masiva
- `[~]` La plantilla se descarga en **Excel real (.xlsx)** generado en el
  cliente con la librería `xlsx`. Acepta también `.csv` y `.xls` para subir.
- `[+]` Capacidad máxima por carga: 500 registros.
- `[+]` Para cada fila se crea una **cuenta ASISTENTE** si no existe (password
  inicial = número de documento) y se inscribe al evento.
- `[+]` Al final se muestra resumen: cuentas creadas, inscripciones, errores
  por fila.

### 3.3 Check-in / Check-out
- `[~]` Solo se puede registrar **el día del evento** y solo si el evento está
  `ACTIVO`.
- `[~]` Solo **una entrada y una salida** por inscripción (no se permiten
  duplicados).
- `[~]` La validación se hace **exclusivamente vía QR**. El botón manual fue
  retirado de la UI.
- `[+]` El staff usa la cámara del dispositivo (se solicita permiso en HTTPS o
  localhost). Se puede elegir cámara delantera/trasera.

---

## 4. Certificados

- `[+]` La generación es **masiva**: el ORGANIZADOR pulsa "Generar" sobre un
  evento finalizado y el backend emite certificados para todos los asistentes
  con estado `ASISTIO`.
- `[+]` Cada certificado tiene un código único y se puede validar
  públicamente con `GET /api/certificados/validar/{codigo}`.
- `[+]` El asistente ve sus certificados en `/mis-certificados` y los puede
  descargar como CSV.

---

## 5. Reportes y dashboards

- `[+]` Dashboard ADMIN: KPIs globales (eventos totales, inscripciones,
  usuarios activos), top eventos populares, reporte avanzado por evento.
- `[+]` Dashboard ORGANIZADOR: KPIs de sus eventos, próximos eventos,
  pendientes (pagos, comprobantes rechazados, etc.).
- `[+]` Dashboard STAFF: eventos asignados y atajos a validador QR.
- `[+]` Exportación de reportes a **Excel y PDF** desde el frontend con
  `xlsx`, `jspdf` y `jspdf-autotable`.
- `[+]` Backend expone también endpoints de exportación nativa
  (`/api/reportes/.../excel` y `.../pdf`) usando Apache POI y OpenPDF.

---

## 6. Notificaciones in-app

- `[~]` Se sustituyen las notificaciones por correo del PDF original por
  **notificaciones in-app** persistidas en base de datos (entidad
  `Notificacion`). Cada notificación tiene tipo INFO/ALERTA/ERROR, fecha,
  destinatario y `leida`.
- `[+]` Disponibles para **todos los roles**. El header del shell muestra una
  campana con badge de no leídas (refresco cada 60 s) y la página
  `/notificaciones` permite marcarlas como leídas, todas o una a una, y filtrar
  por leídas/no leídas.
- `[+]` Eventos que disparan notificación automáticamente:
  - Aprobación / rechazo de organizador.
  - Aprobación / rechazo / cancelación / edición de evento.
  - Aprobación / rechazo de pago.
  - Asignación / desasignación de staff.
  - Carga masiva de asistentes (mensaje al nuevo asistente).
  - Cancelación de evento (a inscritos).
  - Activación / desactivación de cuenta.

---

## 7. Auditoría

- `[+]` Toda acción crítica registra en la tabla `Auditoria` (actor, acción,
  entidad, id, timestamp). El ADMIN tiene una pantalla `/admin/auditoria` para
  consultarlo.

---

## 8. Diferencias visuales / UX

- `[+]` Sistema de diseño global declarado en `frontend/src/styles.scss`
  (variables CSS, clases utilitarias `ez-card`, `ez-btn-*`, `ez-field`,
  `ez-table`, `ez-stat`, `ez-event-card`, `ez-badge-*`).
- `[+]` Layout violeta / canvas blanco para todas las pantallas autenticadas.
- `[+]` Footer minimalista con copyright + versión.
- `[+]` Buscador global del header se muestra solo para roles que tienen
  listados filtrables (ADMIN, ORGANIZADOR, ASISTENTE).
- `[+]` Mensajes de error en formularios detallan exactamente qué campo está
  mal y por qué.

---

## 9. Diferencias técnicas con el diagrama de clases original

- `[~]` Arquitectura final en capas (`controller / service / impl / repository
  / entity / dto`), no se mantuvo la propuesta hexagonal de los primeros
  diagramas.
- `[+]` `ModelMapper` se configura con `MatchingStrategies.STRICT`.
- `[+]` `Notificacion`, `Auditoria` y `Certificado` son entidades de primer
  nivel y no atributos de otras entidades.
- `[+]` `Inscripcion` agrega: `codigoQR`, `fechaCheckIn`, `fechaCheckOut`,
  `tipoCheckIn` (QR / MANUAL), `staffCheckInId`, `staffCheckOutId`.
- `[+]` `Evento` agrega: `fechaFin`, `duracionHoras`, `costo`,
  `motivoRechazo`, `motivoCancelacion`.
- `[+]` `Pago` agrega: `comprobanteUrl`, `motivoRechazo`, `aprobadorId`,
  `fechaResolucion`.

---

## 10. Pendientes de mejora (no bloqueantes)

- HU correspondiente a "envío de credenciales por correo" debe re-redactarse
  como "regenerar contraseña temporal in-app".
- Diagramas de casos de uso deben reflejar el doble ciclo de aprobación
  (evento + pago) y los nuevos casos de "Activar/Desactivar staff (organizador)"
  y "Reactivar usuario (admin)".
- Diagrama de clases debe agregar las relaciones nuevas
  (`Inscripcion.staffCheckIn`, `Pago.aprobador`, `Evento.fechaFin`, etc.).
