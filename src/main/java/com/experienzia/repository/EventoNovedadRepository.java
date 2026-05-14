package com.experienzia.repository;

import com.experienzia.entity.EstadoNovedadEvento;
import com.experienzia.entity.EventoNovedad;
import com.experienzia.entity.TipoNovedadEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventoNovedadRepository extends JpaRepository<EventoNovedad, Long> {

    List<EventoNovedad> findByEventoIdOrderByFechaSolicitudDesc(Long eventoId);

    Optional<EventoNovedad> findFirstByEventoIdAndEstadoOrderByFechaSolicitudDesc(
            Long eventoId, EstadoNovedadEvento estado);

    Optional<EventoNovedad> findFirstByEventoIdAndEstadoAndTipoOrderByFechaSolicitudDesc(
            Long eventoId, EstadoNovedadEvento estado, TipoNovedadEvento tipo);
}
