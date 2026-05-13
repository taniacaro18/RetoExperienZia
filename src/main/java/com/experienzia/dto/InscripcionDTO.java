package com.experienzia.dto;

import com.experienzia.entity.EstadoInscripcion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InscripcionDTO {
    private Long id;
    private Long usuarioId;
    private Long eventoId;
    private LocalDateTime fechaInscripcion;
    private EstadoInscripcion estado;
    private LocalDateTime fechaCheckIn;
    private LocalDateTime fechaCheckOut;
    private String codigoQR;

    /** Completado solo en respuestas de check-in / check-out (QR o manual). */
    private String nombreAsistente;
    private String emailAsistente;
    private String tipoDocumento;
    private String numeroDocumento;
    private String nombreEvento;
    private LocalDateTime fechaEvento;
    private LocalDateTime fechaFinEvento;
    private String ubicacionEvento;
}
