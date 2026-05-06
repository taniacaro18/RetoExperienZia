package com.experienzia.adapters.outbound;

import com.experienzia.domain.model.Inscripcion;
import com.experienzia.domain.port.InscripcionRepository;
import com.experienzia.infrastructure.persistence.entity.InscripcionEntity;
import com.experienzia.infrastructure.persistence.repository.InscripcionJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class InscripcionRepositoryImpl implements InscripcionRepository {

    private final InscripcionJpaRepository jpaRepository;
    private final InscripcionEntityMapper mapper;

    public InscripcionRepositoryImpl(InscripcionJpaRepository jpaRepository, InscripcionEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Inscripcion guardar(Inscripcion inscripcion) {
        InscripcionEntity entity = mapper.toEntity(inscripcion);
        InscripcionEntity guardado = jpaRepository.save(entity);
        return mapper.toDomain(guardado);
    }

    @Override
    public Optional<Inscripcion> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Inscripcion> buscarPorUsuarioYEvento(Long usuarioId, Long eventoId) {
        return jpaRepository.findByUsuarioIdAndEventoId(usuarioId, eventoId).map(mapper::toDomain);
    }

    @Override
    public List<Inscripcion> listarPorEvento(Long eventoId) {
        return jpaRepository.findByEventoId(eventoId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
