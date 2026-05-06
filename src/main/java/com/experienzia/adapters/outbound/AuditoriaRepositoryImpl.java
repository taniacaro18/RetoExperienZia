package com.experienzia.adapters.outbound;

import com.experienzia.domain.model.Auditoria;
import com.experienzia.domain.port.AuditoriaRepository;
import com.experienzia.infrastructure.persistence.entity.AuditoriaEntity;
import com.experienzia.infrastructure.persistence.repository.AuditoriaJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuditoriaRepositoryImpl implements AuditoriaRepository {

    private final AuditoriaJpaRepository jpaRepository;
    private final AuditoriaEntityMapper mapper;

    public AuditoriaRepositoryImpl(AuditoriaJpaRepository jpaRepository, AuditoriaEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Auditoria guardar(Auditoria auditoria) {
        AuditoriaEntity entity = mapper.toEntity(auditoria);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<Auditoria> listarTodo() {
        return jpaRepository.findAllByOrderByFechaDesc().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Auditoria> listarPorUsuario(Long usuarioId) {
        return jpaRepository.findByUsuarioIdOrderByFechaDesc(usuarioId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Auditoria> listarPorEntidad(String entidad) {
        return jpaRepository.findByEntidadOrderByFechaDesc(entidad).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
