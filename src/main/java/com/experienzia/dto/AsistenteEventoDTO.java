package com.experienzia.dto;

import com.experienzia.entity.EstadoInscripcion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsistenteEventoDTO {
    private Long inscripcionId;
    private Long usuarioId;
    private String nombre;
    private String email;
    private String telefono;
    private String tipoDocumento;
    private String numeroDocumento;
    /** Código QR de la inscripción (útil para búsqueda y verificación en staff). */
    private String codigoQR;
    private EstadoInscripcion estadoInscripcion;
    private LocalDateTime fechaInscripcion;
    private LocalDateTime fechaCheckIn;
    private LocalDateTime fechaCheckOut;
}
