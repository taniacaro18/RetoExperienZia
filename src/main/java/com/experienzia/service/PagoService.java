package com.experienzia.service;

import com.experienzia.dto.PagoDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PagoService {

    /**
     * Registra el pago de la tarifa de un evento. Solo lo puede hacer el ORGANIZADOR
     * dueño del evento. El monto se calcula automáticamente desde el costo del evento.
     */
    PagoDTO registrar(Long eventoId, Long organizadorId, MultipartFile archivo);

    PagoDTO aprobar(Long pagoId, Long aprobadorId);

    PagoDTO rechazar(Long pagoId, String motivo, Long aprobadorId);

    List<PagoDTO> listarPendientes();

    List<PagoDTO> listarTodos();

    List<PagoDTO> listarPorOrganizador(Long organizadorId);
}
