package com.experienzia.entity;

/**
 * Enum de tipo de visibilidad del evento (columna en la tabla eventos).
 * Sirve en ExperienZia para distinguir eventos que cualquiera puede encontrar
 * de los que solo ven invitados o personas con enlace.
 */
public enum TipoEvento {
    // Aparece en listados generales para todos los usuarios
    PUBLICO,
    // Acceso restringido, no es para todo el público
    PRIVADO
}
