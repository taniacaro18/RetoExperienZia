package com.experienzia.entity;

/**
 * Enum de estados de una novedad o solicitud de cambio de evento (tabla evento_novedades).
 * Sirve en ExperienZia para que el administrador vea si una petición del organizador
 * sigue en espera, ya fue aceptada o fue rechazada.
 */
public enum EstadoNovedadEvento {
    // El admin aún no decidió sobre la solicitud
    PENDIENTE,
    // El admin aceptó el cambio solicitado
    APROBADO,
    // El admin no permitió el cambio
    RECHAZADO
}
