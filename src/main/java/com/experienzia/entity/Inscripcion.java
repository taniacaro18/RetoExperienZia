package com.experienzia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Representa la tabla "inscripciones" en la base de datos.
 * Une a un usuario (asistente) con un evento al que se registró.
 * Sirve en ExperienZia para saber quién va a asistir, hacer check-in/check-out
 * con el código QR y emitir certificados cuando corresponda.
 */
@Entity
@Table(name = "inscripciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inscripcion {

    // Identificador único de cada inscripción
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Número del usuario que se inscribió
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    // Llave foránea hacia la tabla usuarios: el asistente inscrito
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_inscripcion_usuario"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    // Número del evento al que se inscribió la persona
    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    // Llave foránea hacia la tabla eventos: el evento elegido
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_inscripcion_evento"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Evento evento;

    // Momento en que la persona confirmó su inscripción
    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDateTime fechaInscripcion;

    // Si sigue inscrito, canceló o ya asistió al evento
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoInscripcion estado;

    // Fecha y hora en que entró al evento (check-in)
    @Column(name = "fecha_check_in")
    private LocalDateTime fechaCheckIn;

    // Fecha y hora en que salió del evento (check-out)
    @Column(name = "fecha_check_out")
    private LocalDateTime fechaCheckOut;

    // Código QR único para validar la entrada en puerta
    @Column(name = "codigo_qr", unique = true, length = 64)
    private String codigoQR;
}
