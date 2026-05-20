package com.experienzia.dto;

import com.experienzia.entity.EstadoPago;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Datos de un pago de tarifa de evento.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class PagoDTO {
    /** Identificador unico del registro. */
    private Long id;
    /** Id del evento relacionado. */
    private Long eventoId;
    /** Id del organizador dueño del evento o staff. */
    private Long organizadorId;
    /** Ruta del archivo del comprobante subido. */
    private String comprobanteUrl;
    /** Valor del pago en pesos. */
    private Double monto;
    /** Si no es null, el comprobante pendiente cubre solo la diferencia sobre este monto ya aprobado. */
    private Double saldoAprobadoPrevio;
    /** Estado actual del registro. */
    private EstadoPago estado;
    /** Fecha y hora (inicio o generacion). */
    private LocalDateTime fecha;
    /** Por que se rechazo (evento, pago, etc.). */
    private String motivoRechazo;
    /** Campo aprobador id. */
    private Long aprobadorId;
    /** Cuando el admin resolvio la solicitud. */
    private LocalDateTime fechaResolucion;

    /** Datos extra del evento (solo lectura) para mostrar en la UI sin hacer otra llamada. */
    private String nombreEvento;
    /** Fecha en que ocurrio el evento. */
    private LocalDateTime fechaEvento;
    /** Datos extra del organizador. */
    private String nombreOrganizador;
    /** Campo email organizador. */
    private String emailOrganizador;
}
