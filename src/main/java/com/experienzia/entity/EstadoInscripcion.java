package com.experienzia.entity;

/**
 * Enum de estados de una inscripción (columna "estado" en la tabla inscripciones).
 * Sirve en ExperienZia para saber si el asistente sigue apuntado al evento,
 * si canceló su cupo o si ya asistió y puede recibir certificado.
 */
public enum EstadoInscripcion {
    // La persona tiene cupo reservado en el evento
    INSCRITO,
    // La persona ya no va a asistir
    CANCELADO,
    // La persona entró al evento (check-in hecho)
    ASISTIO
}
