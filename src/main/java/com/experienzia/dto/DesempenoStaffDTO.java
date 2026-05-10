package com.experienzia.dto;

import lombok.Data;

@Data
public class DesempenoStaffDTO {
    private Long staffUsuarioId;
    private String nombre;
    private String funcion;
    private long checkInsRegistrados;
    private long checkOutsRegistrados;
    private long checkInsPorQR;
    private long checkInsManuales;
}
