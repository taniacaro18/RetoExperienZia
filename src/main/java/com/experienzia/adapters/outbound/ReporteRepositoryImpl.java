package com.experienzia.adapters.outbound;

import com.experienzia.application.dto.AsistenciaDTO;
import com.experienzia.application.dto.EventoPopularDTO;
import com.experienzia.application.dto.ResumenDTO;
import com.experienzia.domain.model.EstadoInscripcion;
import com.experienzia.domain.port.ReporteRepository;
import com.experienzia.infrastructure.persistence.EventoJpaRepository;
import com.experienzia.infrastructure.persistence.UsuarioJpaRepository;
import com.experienzia.infrastructure.persistence.repository.InscripcionJpaRepository;
import com.experienzia.infrastructure.persistence.entity.EventoEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReporteRepositoryImpl implements ReporteRepository {

    private final InscripcionJpaRepository inscripcionJpaRepository;
    private final EventoJpaRepository eventoJpaRepository;
    private final UsuarioJpaRepository usuarioJpaRepository;

    public ReporteRepositoryImpl(InscripcionJpaRepository inscripcionJpaRepository,
                                 EventoJpaRepository eventoJpaRepository,
                                 UsuarioJpaRepository usuarioJpaRepository) {
        this.inscripcionJpaRepository = inscripcionJpaRepository;
        this.eventoJpaRepository = eventoJpaRepository;
        this.usuarioJpaRepository = usuarioJpaRepository;
    }

    @Override
    public List<EventoPopularDTO> obtenerEventosPopulares() {
        List<Object[]> resultados = inscripcionJpaRepository.findEventosPopulares();
        return resultados.stream().map(obj -> {
            Long eventoId = (Long) obj[0];
            Long totalInscritos = (Long) obj[1];
            String nombre = eventoJpaRepository.findById(eventoId)
                    .map(EventoEntity::getNombre)
                    .orElse("Evento Desconocido");
            return new EventoPopularDTO(eventoId, nombre, totalInscritos);
        }).collect(Collectors.toList());
    }

    @Override
    public AsistenciaDTO obtenerAsistenciaPorEvento(Long eventoId) {
        long totalAsistieron = inscripcionJpaRepository.countByEventoIdAndEstado(eventoId, EstadoInscripcion.ASISTIO);
        return new AsistenciaDTO(eventoId, totalAsistieron);
    }

    @Override
    public List<Long> obtenerUsuariosPorEvento(Long eventoId) {
        return inscripcionJpaRepository.findUsuarioIdsByEventoId(eventoId);
    }

    @Override
    public ResumenDTO obtenerResumenGeneral() {
        long totalUsuarios = usuarioJpaRepository.count();
        long totalEventos = eventoJpaRepository.count();
        long totalInscripciones = inscripcionJpaRepository.count();
        return new ResumenDTO(totalUsuarios, totalEventos, totalInscripciones);
    }
}
