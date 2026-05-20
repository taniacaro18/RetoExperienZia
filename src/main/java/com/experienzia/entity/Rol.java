package com.experienzia.entity;

/**
 * Enum de roles de usuario (no es una tabla, se guarda como texto en la columna "rol" de usuarios).
 * Define qué puede hacer cada persona en ExperienZia: asistir a eventos, organizarlos,
 * ayudar en puerta como staff o administrar todo el sistema.
 */
public enum Rol {
    // Persona que se inscribe y asiste a eventos
    ASISTENTE,
    // Persona que crea y administra sus propios eventos
    ORGANIZADOR,
    // Persona del equipo del organizador que ayuda en check-in y salida
    STAFF,
    // Persona que aprueba eventos, pagos y usuarios
    ADMIN
}
