package com.experienzia.service;

import com.experienzia.dto.PagoDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Contrato para pagos de tarifa de eventos.
 * El organizador sube el comprobante y el admin lo aprueba o rechaza.
 */
public interface PagoService {

    /**
     * Registra el pago de la tarifa de un evento. Solo lo puede hacer el ORGANIZADOR
     * dueño del evento. El monto se calcula automáticamente desde el costo del evento.
     */
    PagoDTO registrar(Long eventoId, Long organizadorId, MultipartFile archivo, String direccionIp);

    /** El admin aprueba el comprobante y activa el evento si corresponde. */
    PagoDTO aprobar(Long pagoId, Long aprobadorId, String direccionIp);

    /** El admin rechaza el comprobante con un motivo. */
    PagoDTO rechazar(Long pagoId, String motivo, Long aprobadorId, String direccionIp);

    /** Pagos que aun no han sido revisados. */
    List<PagoDTO> listarPendientes();

    /** Todos los pagos del sistema. */
    List<PagoDTO> listarTodos();

    /** Pagos hechos por un organizador. */
    List<PagoDTO> listarPorOrganizador(Long organizadorId);

    /** Busca el pago asociado a un evento (si existe). */
    Optional<PagoDTO> obtenerPorEvento(Long eventoId);
}
