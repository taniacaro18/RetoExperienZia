package com.experienzia.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reporte avanzado del evento usado por las pantallas de "Análisis" del diseño:
 * - curva de ingreso por hora
 * - desglose asistieron / faltaron
 * - desglose por método (QR vs manual)
 * - desempeño por staff
 */
@Data
/**
 * Reporte avanzado con graficos y metricas extra.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class ReporteEventoAvanzadoDTO {
    /** Id del evento relacionado. */
    private Long eventoId;
    /** Campo nombre evento. */
    private String nombreEvento;
    /** Fecha en que ocurrio el evento. */
    private LocalDateTime fechaEvento;
    /** Capacidad maxima de personas. */
    private int aforoMaximo;
    /** Campo inscritos. */
    private long inscritos;
    /** Campo asistieron. */
    private long asistieron;
    /** Campo faltaron. */
    private long faltaron;
    /** Campo porcentaje ocupacion. */
    private double porcentajeOcupacion;
    /** Campo porcentaje asistencia. */
    private double porcentajeAsistencia;
    /** Campo check ins total. */
    private long checkInsTotal;
    /** Campo check ins por q r. */
    private long checkInsPorQR;
    /** Campo check ins manuales. */
    private long checkInsManuales;
    /** Campo check outs total. */
    private long checkOutsTotal;
    /** Campo curva ingreso. */
    private List<CurvaIngresoPuntoDTO> curvaIngreso;
    /** Campo desempeno staff. */
    private List<DesempenoStaffDTO> desempenoStaff;
}
