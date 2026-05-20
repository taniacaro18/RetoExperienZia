package com.experienzia.entity;

/**
 * Enum de estados de un evento (columna "estado" en la tabla eventos).
 * Indica en qué paso del ciclo de vida está cada evento en ExperienZia:
 * desde que el organizador lo crea hasta que termina o se cancela.
 */
public enum EstadoEvento {
    // Evento nuevo que aún no fue aprobado por el admin
    PENDIENTE,
    // El admin aceptó el evento pero puede faltar activarlo con el pago
    APROBADO,
    // El admin no permitió publicar el evento
    RECHAZADO,
    // El evento está publicado y los asistentes pueden inscribirse
    ACTIVO,
    // El evento ya pasó y terminó su fecha
    FINALIZADO,
    // El evento ya no se realizará
    CANCELADO,
    // El organizador editó algo y el admin debe revisar de nuevo
    PENDIENTE_REVISION,
    // Aumentó las horas y debe pagar un extra antes de seguir activo
    PENDIENTE_SUPLEMENTO,
    // El organizador pidió cancelar y el admin debe decidir
    PENDIENTE_CANCELACION
}
