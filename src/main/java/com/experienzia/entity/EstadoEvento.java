package com.experienzia.entity;

public enum EstadoEvento {
    PENDIENTE,
    APROBADO,
    RECHAZADO,
    ACTIVO,
    /** El evento ya ocurrió (ventana de inicio–fin cerrada). */
    FINALIZADO,
    CANCELADO
}
