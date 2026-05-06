package com.experienzia.adapters.outbound;

import com.experienzia.domain.model.EstadoPago;
import com.experienzia.domain.model.Pago;
import com.experienzia.domain.port.PagoRepository;
import com.experienzia.infrastructure.persistence.entity.PagoEntity;
import com.experienzia.infrastructure.persistence.repository.PagoJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PagoRepositoryImpl implements PagoRepository {

    private final PagoJpaRepository jpaRepository;
    private final PagoEntityMapper mapper;

    public PagoRepositoryImpl(PagoJpaRepository jpaRepository, PagoEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Pago guardar(Pago pago) {
        PagoEntity entity = mapper.toEntity(pago);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Pago> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Pago> buscarPorInscripcionId(Long inscripcionId) {
        return jpaRepository.findByInscripcionId(inscripcionId).map(mapper::toDomain);
    }

    @Override
    public List<Pago> listarPendientes() {
        return jpaRepository.findByEstado(EstadoPago.PENDIENTE).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
