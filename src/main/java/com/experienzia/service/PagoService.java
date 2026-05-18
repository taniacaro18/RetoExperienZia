package com.experienzia.service;

import com.experienzia.dto.PagoDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface PagoService {

    /**
     * Registra el pago de la tarifa de un evento. Solo lo puede hacer el ORGANIZADOR
     * dueño del evento. El monto se calcula automáticamente desde el costo del evento.
     */
    PagoDTO registrar(Long eventoId, Long organizadorId, MultipartFile archivo, String direccionIp);

    PagoDTO aprobar(Long pagoId, Long aprobadorId, String direccionIp);

    PagoDTO rechazar(Long pagoId, String motivo, Long aprobadorId, String direccionIp);

    List<PagoDTO> listarPendientes();

    List<PagoDTO> listarTodos();

    List<PagoDTO> listarPorOrganizador(Long organizadorId);

    Optional<PagoDTO> obtenerPorEvento(Long eventoId);
}
