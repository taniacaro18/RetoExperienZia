package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificadoDTO {
    private Long id;
    private Long usuarioId;
    private Long eventoId;
    private LocalDateTime fechaGeneracion;
    private String codigoUnico;

    private String nombreAsistente;
    private String numeroDocumento;
    private String nombreEvento;
    private LocalDateTime fechaEvento;
    private Integer duracionHoras;

    /** Nombre del organizador del evento (firma en el certificado). */
    private String nombreOrganizador;
    /** Ciudad de expedición (p. ej. desde la ubicación del evento). */
    private String ciudadExpedicion;
}
