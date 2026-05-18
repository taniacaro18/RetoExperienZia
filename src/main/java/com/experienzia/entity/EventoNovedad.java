package com.experienzia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Historial de solicitudes de cambio sobre un evento (ediciones, horas, cancelación)
 * para trazabilidad administrativa.
 */
@Entity
@Table(name = "evento_novedades")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoNovedad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    /** FK al evento afectado (solo lectura; el ID se persiste en {@link #eventoId}). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "evento_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_evento_novedad_evento"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Evento evento;

    @Column(name = "usuario_solicitante_id", nullable = false)
    private Long usuarioSolicitanteId;

    /** FK al organizador que solicitó el cambio. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "usuario_solicitante_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_evento_novedad_usuario"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuarioSolicitante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoNovedadEvento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoNovedadEvento estado;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "motivo_resolucion", length = 2000)
    private String motivoResolucion;

    @Column(name = "detalle_json", columnDefinition = "TEXT")
    private String detalleJson;
}
