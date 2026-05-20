package com.experienzia.dto;

import com.experienzia.entity.EstadoInscripcion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Datos de una inscripcion a un evento.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class InscripcionDTO {
    /** Identificador unico del registro. */
    private Long id;
    /** Id del usuario relacionado. */
    private Long usuarioId;
    /** Id del evento relacionado. */
    private Long eventoId;
    /** Campo fecha inscripcion. */
    private LocalDateTime fechaInscripcion;
    /** Estado actual del registro. */
    private EstadoInscripcion estado;
    /** Momento del check-in. */
    private LocalDateTime fechaCheckIn;
    /** Momento del check-out. */
    private LocalDateTime fechaCheckOut;
    /** Codigo QR para hacer check-in. */
    private String codigoQR;

    /** Completado solo en respuestas de check-in / check-out (QR o manual). */
    private String nombreAsistente;
    /** Campo email asistente. */
    private String emailAsistente;
    /** Tipo de documento (CC, CE, etc.). */
    private String tipoDocumento;
    /** Numero del documento de identidad. */
    private String numeroDocumento;
    /** Campo nombre evento. */
    private String nombreEvento;
    /** Fecha en que ocurrio el evento. */
    private LocalDateTime fechaEvento;
    /** Campo fecha fin evento. */
    private LocalDateTime fechaFinEvento;
    /** Campo ubicacion evento. */
    private String ubicacionEvento;
}
