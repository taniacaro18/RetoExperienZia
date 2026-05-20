package com.experienzia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Representa la tabla "pagos" en la base de datos.
 * Guarda el pago que hace el organizador a la plataforma ExperienZia para activar un evento
 * (no es el pago del asistente al organizador).
 * Sirve para que el administrador revise el comprobante, apruebe o rechace,
 * y el evento pueda publicarse o seguir con cambios de horas.
 */
@Entity
@Table(name = "pagos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    // Identificador único del registro de pago
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Número del evento que se está pagando
    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    // Llave foránea hacia la tabla eventos: el evento al que pertenece este pago
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_pago_evento"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Evento evento;

    // Número del usuario organizador que realizó el pago
    @Column(name = "organizador_id", nullable = false)
    private Long organizadorId;

    // Llave foránea hacia la tabla usuarios: el organizador que pagó
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizador_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_pago_organizador"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario organizador;

    // Enlace al archivo del comprobante de pago; puede estar vacío si aún no lo subió
    @Column(name = "comprobante_url", length = 500, nullable = true)
    private String comprobanteUrl;

    // Valor en pesos del pago (por ejemplo precio por hora multiplicado por duración)
    @Column(nullable = false)
    private double monto;

    // Si el pago es un complemento, aquí va el monto que ya estaba aprobado antes
    @Column(name = "saldo_aprobado_previo")
    private Double saldoAprobadoPrevio;

    // Si el pago está pendiente, aprobado o rechazado por el admin
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;

    // Fecha en que se registró el pago
    @Column(nullable = false)
    private LocalDateTime fecha;

    // Texto que explica por qué el admin rechazó el comprobante (si aplica)
    @Column(name = "motivo_rechazo", length = 2000)
    private String motivoRechazo;

    // Número del usuario administrador que aprobó o rechazó
    @Column(name = "aprobador_id")
    private Long aprobadorId;

    // Llave foránea hacia la tabla usuarios: el admin que resolvió el pago
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobador_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_pago_aprobador"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario aprobador;

    // Fecha en que el admin aprobó o rechazó el pago
    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;
}
