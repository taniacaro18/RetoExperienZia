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
/**
 * Objeto para enviar/recibir datos de un evento por la API.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class EventoDTO {
    /** Identificador unico del registro. */
    private Long id;
    /** Nombre (del evento, usuario, etc.). */
    private String nombre;
    /** Descripcion o detalle del evento. */
    private String descripcion;
    /** Fecha y hora (inicio o generacion). */
    private LocalDateTime fecha;
    /** Fecha y hora de fin. */
    private LocalDateTime fechaFin;
    /** Lugar o salon donde es el evento. */
    private String ubicacion;
    /** Si el evento es PUBLICO o PRIVADO. */
    private TipoEvento tipoEvento;
    /** Estado actual del registro. */
    private EstadoEvento estado;
    /** Capacidad maxima de personas. */
    private Integer aforoMaximo;
    /** Cuantas personas estan inscritas ahora. */
    private Integer aforoActual;
    /** Precio o tarifa del evento. */
    private Double costo;
    /** Id del organizador dueño del evento o staff. */
    private Long organizadorId;
    /** Nombre del usuario organizador (contacto para asistentes inscritos). */
    private String organizadorNombre;
    /** Correo del organizador (solo cuando el API no es catálogo público anonimizado). */
    private String organizadorEmail;
    /** URL de la imagen del evento. */
    private String imagen;
    /** Categoria del evento (conferencia, taller, etc.). */
    private String categoria;
    /** Duracion del evento en horas. */
    private Integer duracionHoras;
    /** Por que se rechazo (evento, pago, etc.). */
    private String motivoRechazo;
    /** Por que se cancelo el evento. */
    private String motivoCancelacion;
    /** Qué pidió cambiar el organizador (solo cuando el evento queda PENDIENTE por edición). */
    private String resumenSolicitudEdicion;
    /** Estado previo cuando el evento está en revisión / suplemento / cancelación pendiente. */
    private EstadoEvento estadoPrevioRevision;
    /** Mensaje de negocio para el organizador (no persistido; solo respuesta API). */
    private String alertaNegocio;
}
