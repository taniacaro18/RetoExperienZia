package com.experienzia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Representa la tabla "evento_novedades" en la base de datos.
 * Guarda el historial de solicitudes de cambio sobre un evento (editar datos, horas o cancelar).
 * Sirve en ExperienZia para que el administrador vea qué pidió el organizador,
 * apruebe o rechace, y quede registro de cada solicitud.
 */
@Entity
@Table(name = "evento_novedades")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoNovedad {

    // Identificador único de esta novedad o solicitud
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Número del evento que se quiere modificar
    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    // Llave foránea hacia la tabla eventos: el evento afectado por el cambio
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

    // Número del usuario organizador que envió la solicitud
    @Column(name = "usuario_solicitante_id", nullable = false)
    private Long usuarioSolicitanteId;

    // Llave foránea hacia la tabla usuarios: el organizador que pidió el cambio
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

    // Qué tipo de cambio se solicitó (editar datos, horas, cancelación, etc.)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoNovedadEvento tipo;

    // Si la solicitud está pendiente, aprobada o rechazada
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoNovedadEvento estado;

    // Fecha en que el organizador envió la solicitud
    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    // Fecha en que el admin resolvió la solicitud (si ya la resolvió)
    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    // Comentario del admin al aprobar o rechazar
    @Column(name = "motivo_resolucion", length = 2000)
    private String motivoResolucion;

    // Detalles del cambio en formato texto (JSON) para guardar valores anteriores y nuevos
    @Column(name = "detalle_json", columnDefinition = "TEXT")
    private String detalleJson;
}
