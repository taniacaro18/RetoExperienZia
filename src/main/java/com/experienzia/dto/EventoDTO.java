package com.experienzia.dto;

import com.experienzia.entity.EstadoEvento;
import com.experienzia.entity.TipoEvento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private LocalDateTime fecha;
    private LocalDateTime fechaFin;
    private String ubicacion;
    private TipoEvento tipoEvento;
    private EstadoEvento estado;
    private Integer aforoMaximo;
    private Integer aforoActual;
    private Double costo;
    private Long organizadorId;
    private String imagen;
    private String categoria;
    private Integer duracionHoras;
    private String motivoRechazo;
    private String motivoCancelacion;
}
