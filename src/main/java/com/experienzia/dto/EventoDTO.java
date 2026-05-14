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
    /** Nombre del usuario organizador (contacto para asistentes inscritos). */
    private String organizadorNombre;
    /** Correo del organizador (solo cuando el API no es catálogo público anonimizado). */
    private String organizadorEmail;
    private String imagen;
    private String categoria;
    private Integer duracionHoras;
    private String motivoRechazo;
    private String motivoCancelacion;
    /** Qué pidió cambiar el organizador (solo cuando el evento queda PENDIENTE por edición). */
    private String resumenSolicitudEdicion;
    /** Estado previo cuando el evento está en revisión / suplemento / cancelación pendiente. */
    private EstadoEvento estadoPrevioRevision;
    /** Mensaje de negocio para el organizador (no persistido; solo respuesta API). */
    private String alertaNegocio;
}
