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
public class ReporteEventoAvanzadoDTO {
    private Long eventoId;
    private String nombreEvento;
    private LocalDateTime fechaEvento;
    private int aforoMaximo;
    private long inscritos;
    private long asistieron;
    private long faltaron;
    private double porcentajeOcupacion;
    private double porcentajeAsistencia;
    private long checkInsTotal;
    private long checkInsPorQR;
    private long checkInsManuales;
    private long checkOutsTotal;
    private List<CurvaIngresoPuntoDTO> curvaIngreso;
    private List<DesempenoStaffDTO> desempenoStaff;
}
