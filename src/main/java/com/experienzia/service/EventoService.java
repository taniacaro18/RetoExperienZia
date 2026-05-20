package com.experienzia.service;

import com.experienzia.dto.DisponibilidadSalonDTO;
import com.experienzia.dto.EventoDTO;
import com.experienzia.dto.EventoNovedadDTO;
import com.experienzia.spec.EventoSpecification.EventoSearchCriteria;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrato del servicio de eventos.
 * Aqui definimos que operaciones puede hacer la app con los eventos (crear, aprobar, cancelar, etc.).
 */
public interface EventoService {

    /** Crea un evento nuevo en estado PENDIENTE. */
    EventoDTO crear(EventoDTO dto);

    /** Edita un evento existente; puede quedar en revision segun los cambios. */
    EventoDTO editar(Long id, EventoDTO dto);

    /** El admin aprueba un evento PENDIENTE o una edicion pendiente. */
    EventoDTO aprobar(Long id);

    /** El admin rechaza un evento o una edicion; puede revertir cambios. */
    EventoDTO rechazar(Long id, String motivo);

    /** El organizador pide cancelar su evento. */
    EventoDTO cancelar(Long id, Long organizadorId, String motivo);
    /** Admin: aprueba la cancelación solicitada por el organizador. */
    EventoDTO aprobarCancelacion(Long id);
    /** Admin: rechaza la cancelación y restaura el estado previo. */
    EventoDTO rechazarCancelacion(Long id, String motivo);
    /** Pasa el evento de APROBADO a ACTIVO cuando el pago fue validado. */
    EventoDTO activarPorPago(Long id);
    /** Tras aprobar el comprobante del suplemento por horas adicionales, reactiva el evento. */
    EventoDTO activarTrasSuplementoPago(Long eventoId);
    /**
     * Tras aprobar el comprobante de un complemento cuando el evento sigue ACTIVO
     * (delta sobre saldo ya aprobado). No cambia el estado del evento; resuelve novedades de suplemento si aplica.
     */
    void resolverComplementoPagoSobreEventoActivo(Long eventoId);
    /** Busca un evento por su id. */
    EventoDTO obtenerPorId(Long id);

    /** Lista todos los eventos (normalmente lo usa el admin). */
    List<EventoDTO> listarTodos();

    /** Eventos publicos activos para el catalogo sin login. */
    List<EventoDTO> listarCatalogoPublicoActivo();
    /** Detalle público: solo evento público, activo y cuya ventana horaria aún no terminó. */
    EventoDTO obtenerParaCatalogoPublico(Long id);
    /** Eventos creados por un organizador especifico. */
    List<EventoDTO> listarPorOrganizador(Long organizadorId);

    /** Busqueda con filtros (nombre, categoria, fechas, etc.). */
    List<EventoDTO> buscar(EventoSearchCriteria criteria);

    /** Historial de novedades/solicitudes de cambio de un evento. */
    List<EventoNovedadDTO> listarNovedades(Long eventoId);

    /** Suma 1 al aforo actual cuando alguien se inscribe. */
    void aumentarAforo(Long eventoId);

    /** Resta 1 al aforo actual cuando alguien cancela inscripcion. */
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
