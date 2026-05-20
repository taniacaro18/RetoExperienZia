package com.experienzia.entity;

/**
 * Enum de tipo de notificación (columna en la tabla notificaciones).
 * Sirve en ExperienZia para mostrar el aviso con el color o icono adecuado
 * según sea un mensaje normal, una alerta importante o un error.
 */
public enum TipoNotificacion {
    // Mensaje informativo sin urgencia
    INFO,
    // Aviso que requiere atención del usuario
    ALERTA,
    // Algo salió mal o fue rechazado
    ERROR
}
