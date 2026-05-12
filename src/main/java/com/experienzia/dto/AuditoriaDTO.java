package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaDTO {
    private Long id;
    private Long usuarioId;
    private String accion;
    private String entidad;
    private Long entidadId;
    private LocalDateTime fecha;
    private String direccionIp;
}
