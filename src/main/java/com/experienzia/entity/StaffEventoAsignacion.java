package com.experienzia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "staff_evento_asignaciones",
        uniqueConstraints = @UniqueConstraint(name = "uk_staff_evento", columnNames = {"staff_usuario_id", "evento_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffEventoAsignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_usuario_id", nullable = false)
    private Long staffUsuarioId;

    /** FK al usuario staff (solo para BD/ERD). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_usuario_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_asignacion_staff"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario staff;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    /** FK al evento (solo para BD/ERD). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_asignacion_evento"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Evento evento;

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
