package com.experienzia.entity;

/**
 * Enum de estados de una cuenta de usuario (columna "estado" en la tabla usuarios).
 * Sirve en ExperienZia para saber si alguien puede entrar a la app o si aún espera
 * aprobación del administrador (por ejemplo un organizador nuevo).
 */
public enum Estado {
    // La cuenta funciona con normalidad
    ACTIVO,
    // Recién registrada y esperando que el admin la apruebe
    PENDIENTE,
    // El admin no autorizó la cuenta
    RECHAZADO,
    // Cuenta desactivada o bloqueada
    INACTIVO
}
