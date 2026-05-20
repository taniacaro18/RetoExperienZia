package com.experienzia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Representa la tabla "certificados" en la base de datos.
 * Guarda el certificado que recibe un asistente después de asistir a un evento.
 * Sirve en ExperienZia para demostrar participación con un código único verificable.
 */
@Entity
@Table(name = "certificados")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Certificado {

    // Identificador único del certificado
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Número del usuario que recibió el certificado
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    // Llave foránea hacia la tabla usuarios: la persona titular del certificado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_certificado_usuario"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    // Número del evento por el que se otorgó el certificado
    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    // Llave foránea hacia la tabla eventos: el evento relacionado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_certificado_evento"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Evento evento;

    // Momento en que el sistema generó el certificado
    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    // Código alfanumérico único para validar que el certificado es auténtico
    @Column(name = "codigo_unico", nullable = false, unique = true, length = 100)
    private String codigoUnico;
}
