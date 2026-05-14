package com.experienzia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "eventos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 1000)
    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(length = 200)
    private String ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEvento tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EstadoEvento estado;

    /**
     * Cuando el evento entra en {@link EstadoEvento#PENDIENTE_REVISION},
     * {@link EstadoEvento#PENDIENTE_SUPLEMENTO} o {@link EstadoEvento#PENDIENTE_CANCELACION},
     * guarda el estado anterior para restaurarlo al aprobar/rechazar según reglas.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_previo_revision", length = 32)
    private EstadoEvento estadoPrevioRevision;

    @Column(nullable = false)
    private int aforoMaximo;

    @Column(nullable = false)
    private int aforoActual;

    @Column(nullable = false)
    private double costo;

    @Column(name = "organizador_id", nullable = false)
    private Long organizadorId;

    /** FK al usuario organizador (solo para que la BD/ERD muestren la relación). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizador_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_evento_organizador"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario organizador;

    private String imagen;

    @Column(length = 100)
    private String categoria;

    @Column(name = "duracion_horas")
    private Integer duracionHoras;

    @Column(name = "motivo_rechazo", length = 2000)
    private String motivoRechazo;

    @Column(name = "motivo_cancelacion", length = 2000)
    private String motivoCancelacion;

    /**
     * Texto breve para el administrador: qué cambió en la última edición que requirió re-aprobación.
     * Se limpia al aprobar el evento.
     */
    @Column(name = "resumen_solicitud_edicion", length = 2000)
    private String resumenSolicitudEdicion;
}
