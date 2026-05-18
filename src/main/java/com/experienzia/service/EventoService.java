package com.experienzia.service;

import com.experienzia.dto.DisponibilidadSalonDTO;
import com.experienzia.dto.EventoDTO;
import com.experienzia.dto.EventoNovedadDTO;
import com.experienzia.spec.EventoSpecification.EventoSearchCriteria;

import java.time.LocalDateTime;
import java.util.List;

public interface EventoService {
    EventoDTO crear(EventoDTO dto);
    EventoDTO editar(Long id, EventoDTO dto);
    EventoDTO aprobar(Long id);
    EventoDTO rechazar(Long id, String motivo);
    EventoDTO cancelar(Long id, Long organizadorId, String motivo);
    /** Admin: aprueba la cancelación solicitada por el organizador. */
    EventoDTO aprobarCancelacion(Long id);
    /** Admin: rechaza la cancelación y restaura el estado previo. */
    EventoDTO rechazarCancelacion(Long id, String motivo);
    EventoDTO activarPorPago(Long id);
    /** Tras aprobar el comprobante del suplemento por horas adicionales, reactiva el evento. */
    EventoDTO activarTrasSuplementoPago(Long eventoId);
    /**
     * Tras aprobar el comprobante de un complemento cuando el evento sigue ACTIVO
     * (delta sobre saldo ya aprobado). No cambia el estado del evento; resuelve novedades de suplemento si aplica.
     */
    void resolverComplementoPagoSobreEventoActivo(Long eventoId);
    EventoDTO obtenerPorId(Long id);
    List<EventoDTO> listarTodos();
    List<EventoDTO> listarCatalogoPublicoActivo();
    /** Detalle público: solo evento público, activo y cuya ventana horaria aún no terminó. */
    EventoDTO obtenerParaCatalogoPublico(Long id);
    List<EventoDTO> listarPorOrganizador(Long organizadorId);
    List<EventoDTO> buscar(EventoSearchCriteria criteria);

    List<EventoNovedadDTO> listarNovedades(Long eventoId);

    void aumentarAforo(Long eventoId);
    void disminuirAforo(Long eventoId);

    /** Marca FINALIZADOS los eventos ACTIVO cuya fecha/hora de fin ya pasó. */
    void marcarEventosActivosFinalizados();

    /**
     * Ocupación del salón en un rango (calendario). Opcionalmente valida una franja propuesta.
     */
    DisponibilidadSalonDTO consultarDisponibilidadSalon(
            String ubicacion,
            LocalDateTime desde,
            LocalDateTime hasta,
            Long excluirEventoId,
            LocalDateTime propuestaInicio,
            LocalDateTime propuestaFin);
}
