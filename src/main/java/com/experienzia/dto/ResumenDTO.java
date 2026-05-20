package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Resumen general de metricas del sistema.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class ResumenDTO {
    /** Campo total usuarios. */
    private Long totalUsuarios;
    /** Campo total eventos. */
    private Long totalEventos;
    /** Campo total inscripciones. */
    private Long totalInscripciones;
}
