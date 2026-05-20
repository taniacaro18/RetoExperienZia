package com.experienzia.repository;

import com.experienzia.entity.EstadoNovedadEvento;
import com.experienzia.entity.EventoNovedad;
import com.experienzia.entity.TipoNovedadEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para novedades/solicitudes de cambio de eventos.
 */
public interface EventoNovedadRepository extends JpaRepository<EventoNovedad, Long> {

    /** Novedades de un evento, las mas recientes primero. */
    List<EventoNovedad> findByEventoIdOrderByFechaSolicitudDesc(Long eventoId);

    /** Ultima novedad pendiente de un evento. */
    Optional<EventoNovedad> findFirstByEventoIdAndEstadoOrderByFechaSolicitudDesc(
            Long eventoId, EstadoNovedadEvento estado);

    /** Ultima novedad pendiente de un tipo especifico (aumento horas, cancelacion, etc.). */
    Optional<EventoNovedad> findFirstByEventoIdAndEstadoAndTipoOrderByFechaSolicitudDesc(
            Long eventoId, EstadoNovedadEvento estado, TipoNovedadEvento tipo);
}
