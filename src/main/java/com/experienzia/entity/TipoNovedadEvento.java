package com.experienzia.entity;

/**
 * Enum de tipos de solicitud de cambio sobre un evento (tabla evento_novedades).
 * Sirve en ExperienZia para que el administrador entienda qué pidió cambiar el organizador
 * (datos del evento, horas, cancelación, etc.) y aplique las reglas correctas.
 */
public enum TipoNovedadEvento {
    // Cambio en nombre, descripción, ubicación u otros datos básicos
    EDICION_METADATOS,
    // Cambio entre público/privado o la categoría
    EDICION_TIPO_CATEGORIA,
    // El organizador quiere que el evento dure más horas
    AUMENTO_HORAS,
    // El organizador quiere reducir las horas del evento
    DISMINUCION_HORAS,
    // El organizador pide cancelar el evento
    CANCELACION_SOLICITUD
}
