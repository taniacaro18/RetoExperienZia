package com.experienzia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(name = "usuario_solicitante_id", nullable = false)
    private Long usuarioSolicitanteId;

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
