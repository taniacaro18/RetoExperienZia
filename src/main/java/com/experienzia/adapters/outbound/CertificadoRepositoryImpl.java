package com.experienzia.adapters.outbound;

import com.experienzia.domain.model.Certificado;
import com.experienzia.domain.port.CertificadoRepository;
import com.experienzia.infrastructure.persistence.entity.CertificadoEntity;
import com.experienzia.infrastructure.persistence.repository.CertificadoJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CertificadoRepositoryImpl implements CertificadoRepository {

    private final CertificadoJpaRepository jpaRepository;
    private final CertificadoEntityMapper mapper;

    public CertificadoRepositoryImpl(CertificadoJpaRepository jpaRepository, CertificadoEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Certificado guardar(Certificado certificado) {
        CertificadoEntity entity = mapper.toEntity(certificado);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<Certificado> listarPorUsuario(Long usuarioId) {
        return jpaRepository.findByUsuarioId(usuarioId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Certificado> buscarPorCodigo(String codigo) {
        return jpaRepository.findByCodigoUnico(codigo).map(mapper::toDomain);
    }

    @Override
    public Optional<Certificado> buscarPorUsuarioYEvento(Long usuarioId, Long eventoId) {
        return jpaRepository.findByUsuarioIdAndEventoId(usuarioId, eventoId).map(mapper::toDomain);
    }
}
