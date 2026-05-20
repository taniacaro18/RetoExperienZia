package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Certificado de asistencia a un evento.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class CertificadoDTO {
    /** Identificador unico del registro. */
    private Long id;
    /** Id del usuario relacionado. */
    private Long usuarioId;
    /** Id del evento relacionado. */
    private Long eventoId;
    /** Cuando se genero el certificado u otro registro. */
    private LocalDateTime fechaGeneracion;
    /** Codigo unico del certificado para validar. */
    private String codigoUnico;

    /** Campo nombre asistente. */
    private String nombreAsistente;
    /** Numero del documento de identidad. */
    private String numeroDocumento;
    /** Campo nombre evento. */
    private String nombreEvento;
    /** Fecha en que ocurrio el evento. */
    private LocalDateTime fechaEvento;
    /** Duracion del evento en horas. */
    private Integer duracionHoras;

    /** Nombre del organizador del evento (firma en el certificado). */
    private String nombreOrganizador;
    /** Ciudad de expedición (p. ej. desde la ubicación del evento). */
    private String ciudadExpedicion;
}
