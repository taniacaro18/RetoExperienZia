package com.experienzia.entity;

/**
 * Función específica que cumple un STAFF dentro de un evento (visible en el diseño Figma).
 * - CHECK_IN_QR: lectura de QR de asistentes en el ingreso.
 * - CHECK_IN_MANUAL: registro manual de asistentes en el ingreso.
 * - REGISTRO_SALIDA: marca de check-out a la salida del evento.
 * - GENERAL: cualquier responsabilidad combinada (valor por defecto si no se especifica).
 */
public enum FuncionStaff {
    CHECK_IN_QR,
    CHECK_IN_MANUAL,
    REGISTRO_SALIDA,
    GENERAL
}
