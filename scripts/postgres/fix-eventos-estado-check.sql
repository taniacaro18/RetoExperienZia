-- Ejecutar manualmente si no usas el arreglo al arranque (PostgresEventoEstadoCheckFix).
-- Corrige: ERROR: violates check constraint "eventos_estado_check" al guardar estado FINALIZADO.

ALTER TABLE eventos DROP CONSTRAINT IF EXISTS eventos_estado_check;

ALTER TABLE eventos ADD CONSTRAINT eventos_estado_check CHECK (
    estado IN (
        'PENDIENTE',
        'APROBADO',
        'RECHAZADO',
        'ACTIVO',
        'FINALIZADO',
        'CANCELADO'
    )
);
