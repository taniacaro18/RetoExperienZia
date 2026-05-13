package com.experienzia.service;

import com.experienzia.dto.EventoDTO;
import com.experienzia.spec.EventoSpecification.EventoSearchCriteria;

import java.util.List;

public interface EventoService {
    EventoDTO crear(EventoDTO dto);
    EventoDTO editar(Long id, EventoDTO dto);
    EventoDTO aprobar(Long id);
    EventoDTO rechazar(Long id, String motivo);
    EventoDTO cancelar(Long id, Long organizadorId, String motivo);
    EventoDTO activarPorPago(Long id);
    EventoDTO obtenerPorId(Long id);
    List<EventoDTO> listarTodos();
    List<EventoDTO> listarCatalogoPublicoActivo();
    /** Detalle público: solo evento público, activo y cuya ventana horaria aún no terminó. */
    EventoDTO obtenerParaCatalogoPublico(Long id);
    List<EventoDTO> listarPorOrganizador(Long organizadorId);
    List<EventoDTO> buscar(EventoSearchCriteria criteria);
    void aumentarAforo(Long eventoId);
    void disminuirAforo(Long eventoId);

    /** Marca FINALIZADOS los eventos ACTIVO cuya fecha/hora de fin ya pasó. */
    void marcarEventosActivosFinalizados();
}
