package com.experienzia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Representa la tabla "notificaciones" en la base de datos.
 * Guarda los avisos que ve cada usuario dentro de ExperienZia (mensajes de info, alertas o errores).
 * Sirve para informar cambios en eventos, pagos, inscripciones u otras acciones importantes.
 */
@Entity
@Table(name = "notificaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    // Identificador único de la notificación
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Número del usuario que debe leer esta notificación
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    // Llave foránea hacia la tabla usuarios: el destinatario del mensaje
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_notificacion_usuario"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    // Texto que aparece en la campana o lista de notificaciones
    @Column(nullable = false, length = 500)
    private String mensaje;

    // Tipo de aviso: informativo, alerta o error
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacion tipo;

    // true si el usuario ya abrió o marcó como leída la notificación
    @Column(nullable = false)
    private boolean leida;

    // Fecha y hora en que se creó la notificación
    @Column(nullable = false)
    private LocalDateTime fecha;
}
