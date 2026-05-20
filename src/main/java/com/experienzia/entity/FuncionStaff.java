package com.experienzia.entity;

/**
 * Enum de la función del STAFF en un evento concreto (columna en staff_evento_asignaciones).
 * Sirve en ExperienZia para limitar qué pantallas puede usar el ayudante del organizador:
 * leer QR, registrar manualmente o marcar salidas.
 */
public enum FuncionStaff {
    // Escanear el código QR del asistente al entrar
    CHECK_IN_QR,
    // Registrar la entrada escribiendo datos sin QR
    CHECK_IN_MANUAL,
    // Marcar cuando el asistente sale del evento
    REGISTRO_SALIDA,
    // Puede hacer varias tareas; valor por defecto si no se especifica otra
    GENERAL
}
