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
 * Asistente inscrito con info para el staff.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class AsistenteEventoDTO {
    /** Id de la inscripcion. */
    private Long inscripcionId;
    /** Id del usuario relacionado. */
    private Long usuarioId;
    /** Nombre (del evento, usuario, etc.). */
    private String nombre;
    /** Correo electronico del usuario. */
    private String email;
    /** Numero de telefono de contacto. */
    private String telefono;
    /** Tipo de documento (CC, CE, etc.). */
    private String tipoDocumento;
    /** Numero del documento de identidad. */
    private String numeroDocumento;
    /** Código QR de la inscripción (útil para búsqueda y verificación en staff). */
    private String codigoQR;
    /** Campo estado inscripcion. */
    private EstadoInscripcion estadoInscripcion;
    /** Campo fecha inscripcion. */
    private LocalDateTime fechaInscripcion;
    /** Momento del check-in. */
    private LocalDateTime fechaCheckIn;
    /** Momento del check-out. */
    private LocalDateTime fechaCheckOut;
}
