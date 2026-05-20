package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Estadisticas de asistencia de un evento.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class AsistenciaDTO {
    /** Id del evento relacionado. */
    private Long eventoId;
    /** Campo total asistieron. */
    private Long totalAsistieron;
}
