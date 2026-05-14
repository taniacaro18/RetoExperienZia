package com.experienzia.dto;

import com.experienzia.entity.EstadoPago;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoDTO {
    private Long id;
    private Long eventoId;
    private Long organizadorId;
    private String comprobanteUrl;
    private Double monto;
    /** Si no es null, el comprobante pendiente cubre solo la diferencia sobre este monto ya aprobado. */
    private Double saldoAprobadoPrevio;
    private EstadoPago estado;
    private LocalDateTime fecha;
    private String motivoRechazo;
    private Long aprobadorId;
    private LocalDateTime fechaResolucion;

    /** Datos extra del evento (solo lectura) para mostrar en la UI sin hacer otra llamada. */
    private String nombreEvento;
    private LocalDateTime fechaEvento;
    /** Datos extra del organizador. */
    private String nombreOrganizador;
    private String emailOrganizador;
}
