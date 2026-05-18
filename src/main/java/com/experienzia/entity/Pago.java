package com.experienzia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Pago de la tarifa de la plataforma para activar un evento.
 * Lo realiza siempre el ORGANIZADOR (no el asistente). El monto se calcula
 * automáticamente como precioPorHora * duracionHoras del evento.
 */
@Entity
@Table(name = "pagos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    /** FK al evento que se está pagando. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_pago_evento"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Evento evento;

    @Column(name = "organizador_id", nullable = false)
    private Long organizadorId;

    /** FK al organizador que pagó (debe ser dueño del evento). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizador_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_pago_organizador"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario organizador;

    /** Null cuando el organizador aún no subió comprobante o tras cambio de tarifa / suplemento. */
    @Column(name = "comprobante_url", length = 500, nullable = true)
    private String comprobanteUrl;

    /** Monto del pago en COP (precioPorHora * duracionHoras del evento). */
    @Column(nullable = false)
    private double monto;

    /**
     * Si no es null, el pago en PENDIENTE es un complemento: {@link #monto} es solo el incremento
     * y este campo guarda el monto ya aprobado previamente (se suman al aprobar el complemento).
     */
    @Column(name = "saldo_aprobado_previo")
    private Double saldoAprobadoPrevio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "motivo_rechazo", length = 2000)
    private String motivoRechazo;

    @Column(name = "aprobador_id")
    private Long aprobadorId;

    /** FK al admin que aprobó/rechazó (solo para BD/ERD). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobador_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_pago_aprobador"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario aprobador;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;
}
