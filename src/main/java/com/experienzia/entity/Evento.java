package com.experienzia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Representa la tabla "eventos" en la base de datos.
 * Cada fila es un evento que un organizador crea en ExperienZia (taller, charla, etc.).
 * Sirve para mostrar eventos al público, controlar cupos, precios, estados de aprobación
 * y vincular inscripciones, pagos y certificados con ese evento.
 */
@Entity
@Table(name = "eventos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Evento {

    // Identificador único del evento
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Título o nombre del evento
    @Column(nullable = false, length = 200)
    private String nombre;

    // Texto que explica de qué trata el evento
    @Column(length = 1000)
    private String descripcion;

    // Fecha y hora de inicio del evento
    @Column(nullable = false)
    private LocalDateTime fecha;

    // Fecha y hora de fin (si el evento dura varias horas)
    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    // Lugar donde se realiza (salón, dirección, enlace, etc.)
    @Column(length = 200)
    private String ubicacion;

    // Si el evento es público (cualquiera puede verlo) o privado
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEvento tipoEvento;

    // Estado actual del evento en el flujo (pendiente, activo, cancelado, etc.)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EstadoEvento estado;

    // Estado que tenía el evento antes de pedir una revisión; sirve para volver atrás si el admin aprueba
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_previo_revision", length = 32)
    private EstadoEvento estadoPrevioRevision;

    // Cantidad máxima de personas que pueden inscribirse
    @Column(nullable = false)
    private int aforoMaximo;

    // Cuántas personas ya están inscritas (para no pasar el límite)
    @Column(nullable = false)
    private int aforoActual;

    // Precio o costo del evento para el asistente (si aplica)
    @Column(nullable = false)
    private double costo;

    // Número del usuario organizador que creó el evento
    @Column(name = "organizador_id", nullable = false)
    private Long organizadorId;

    // Llave foránea hacia la tabla usuarios: el organizador dueño de este evento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizador_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_evento_organizador"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario organizador;

    // Ruta o URL de la imagen del cartel del evento
    private String imagen;

    // Categoría del evento (deporte, cultura, etc.)
    @Column(length = 100)
    private String categoria;

    // Cuántas horas dura el evento (se usa para calcular el pago a la plataforma)
    @Column(name = "duracion_horas")
    private Integer duracionHoras;

    // Motivo que escribió el admin si rechazó el evento
    @Column(name = "motivo_rechazo", length = 2000)
    private String motivoRechazo;

    // Motivo de cancelación del evento
    @Column(name = "motivo_cancelacion", length = 2000)
    private String motivoCancelacion;

    // Resumen corto para el admin de qué cambió en la última edición que pide nueva aprobación
    @Column(name = "resumen_solicitud_edicion", length = 2000)
    private String resumenSolicitudEdicion;
}
