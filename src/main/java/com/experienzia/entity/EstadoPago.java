package com.experienzia.entity;

/**
 * Enum de estados de un pago del organizador a la plataforma (tabla pagos).
 * Sirve en ExperienZia para que el administrador sepa si debe revisar el comprobante,
 * si ya aprobó el pago y el evento puede activarse, o si lo rechazó.
 */
public enum EstadoPago {
    // Comprobante enviado o pendiente de revisión del admin
    PENDIENTE,
    // El admin validó el pago
    APROBADO,
    // El admin no aceptó el comprobante
    RECHAZADO
}
