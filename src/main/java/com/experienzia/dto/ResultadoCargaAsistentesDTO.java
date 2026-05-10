package com.experienzia.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ResultadoCargaAsistentesDTO {
    private int cuentasNuevasCreadas;
    private int inscripcionesRegistradas;
    private int filasOmitidasDuplicadoUOtros;
    private List<String> errores = new ArrayList<>();
}
