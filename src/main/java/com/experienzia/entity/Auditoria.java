package com.experienzia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Representa la tabla "auditorias" en la base de datos.
 * Registra quién hizo qué acción en el sistema y cuándo (bitácora de seguridad).
 * Sirve en ExperienZia para revisar cambios importantes y saber desde qué IP se hicieron.
 */
@Entity
@Table(name = "auditorias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Auditoria {

    // Identificador único del registro de auditoría
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Número del usuario que realizó la acción (puede ser vacío si fue el sistema)
    @Column(name = "usuario_id")
    private Long usuarioId;

    // Llave foránea hacia la tabla usuarios: la persona que ejecutó la acción
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_auditoria_usuario"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    // Descripción corta de lo que hizo, por ejemplo "APROBAR_EVENTO"
    @Column(nullable = false)
    private String accion;

    // Nombre de la entidad o tabla afectada, por ejemplo "Evento"
    @Column(nullable = false)
    private String entidad;

    // Identificador del registro concreto que se modificó
    @Column(name = "entidad_id")
    private Long entidadId;

    // Momento exacto en que ocurrió la acción
    @Column(nullable = false)
    private LocalDateTime fecha;

    // Dirección IP del dispositivo desde donde se hizo la petición
    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;
}
