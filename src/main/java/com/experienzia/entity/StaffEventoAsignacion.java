package com.experienzia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Representa la tabla "staff_evento_asignaciones" en la base de datos.
 * Indica qué usuario STAFF está asignado a qué evento y con qué función (check-in, salida, etc.).
 * Sirve en ExperienZia para que el organizador delegue tareas en puerta sin dar acceso total al evento.
 */
@Entity
@Table(name = "staff_evento_asignaciones",
        uniqueConstraints = @UniqueConstraint(name = "uk_staff_evento", columnNames = {"staff_usuario_id", "evento_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffEventoAsignacion {

    // Identificador único de la asignación
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Número del usuario con rol STAFF que ayuda en el evento
    @Column(name = "staff_usuario_id", nullable = false)
    private Long staffUsuarioId;

    // Llave foránea hacia la tabla usuarios: el miembro del staff
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_usuario_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_asignacion_staff"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario staff;

    // Número del evento donde trabaja ese staff
    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    // Llave foránea hacia la tabla eventos: el evento asignado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_asignacion_evento"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Evento evento;

    // Tarea concreta del staff en ese evento (QR, manual, salida o general)
    @Enumerated(EnumType.STRING)
    @Column(name = "funcion", length = 30, nullable = false)
    private FuncionStaff funcion = FuncionStaff.GENERAL;

    public StaffEventoAsignacion(Long staffUsuarioId, Long eventoId) {
        this.staffUsuarioId = staffUsuarioId;
        this.eventoId = eventoId;
        this.funcion = FuncionStaff.GENERAL;
    }

    public StaffEventoAsignacion(Long staffUsuarioId, Long eventoId, FuncionStaff funcion) {
        this.staffUsuarioId = staffUsuarioId;
        this.eventoId = eventoId;
        this.funcion = funcion == null ? FuncionStaff.GENERAL : funcion;
    }
}
