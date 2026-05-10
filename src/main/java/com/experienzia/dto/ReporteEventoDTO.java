package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteEventoDTO {
    private Long eventoId;
    private String nombreEvento;
    private String estadoEvento;
    private LocalDateTime fechaEvento;
    private Integer duracionHoras;
    private int aforoMaximo;
    private long inscritos;
    private long asistenciasReales;
    private long asistentesActualmenteEnSala;
    private double porcentajeOcupacion;
    private double porcentajeAsistenciaSobreInscritos;
    private List<AsistenteEventoDTO> asistentes;
}
