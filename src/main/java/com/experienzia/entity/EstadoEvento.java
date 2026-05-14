package com.experienzia.entity;

public enum EstadoEvento {
    /** Solicitud inicial de evento nuevo (sin pago aún o flujo clásico). */
    PENDIENTE,
    APROBADO,
    RECHAZADO,
    ACTIVO,
    /** El evento ya ocurrió (ventana de inicio–fin cerrada). */
    FINALIZADO,
    CANCELADO,
    /**
     * Edición de un evento ya activo/aprobado que no implica suplemento de pago por horas
     * (o reducción de horas con penalización): requiere aprobación administrativa.
     */
    PENDIENTE_REVISION,
    /** Aumento de horas con pago ya aprobado: debe pagarse solo el excedente y aprobarse el comprobante. */
    PENDIENTE_SUPLEMENTO,
    /** Solicitud de cancelación por el organizador; el admin aprueba o rechaza. */
    PENDIENTE_CANCELACION
}
