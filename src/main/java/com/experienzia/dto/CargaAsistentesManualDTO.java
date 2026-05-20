package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Lista manual de asistentes a cargar.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class CargaAsistentesManualDTO {
    /** Id del organizador dueño del evento o staff. */
    private Long organizadorId;
    /** Filas de datos para carga masiva. */
    private List<FilaAsistenteCargaDTO> filas;
}
