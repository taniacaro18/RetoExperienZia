package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenDTO {
    private Long totalUsuarios;
    private Long totalEventos;
    private Long totalInscripciones;
}
