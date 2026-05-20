package com.experienzia.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
/**
 * Resultado de cargar asistentes (exitos y errores).
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class ResultadoCargaAsistentesDTO {
    /** Campo cuentas nuevas creadas. */
    private int cuentasNuevasCreadas;
    /** Campo inscripciones registradas. */
    private int inscripcionesRegistradas;
    /** Campo filas omitidas duplicado u otros. */
    private int filasOmitidasDuplicadoUOtros;
    private List<String> errores = new ArrayList<>();
}
