package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Registro de auditoria de acciones en el sistema.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class AuditoriaDTO {
    /** Identificador unico del registro. */
    private Long id;
    /** Id del usuario relacionado. */
    private Long usuarioId;
    /** Que accion se registro en auditoria. */
    private String accion;
    /** Nombre de la tabla o entidad afectada. */
    private String entidad;
    /** Id del registro afectado en auditoria. */
    private Long entidadId;
    /** Fecha y hora (inicio o generacion). */
    private LocalDateTime fecha;
    /** IP desde donde se hizo la accion. */
    private String direccionIp;
}
