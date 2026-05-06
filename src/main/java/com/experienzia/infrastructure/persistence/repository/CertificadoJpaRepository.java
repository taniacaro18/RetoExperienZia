package com.experienzia.infrastructure.persistence.repository;

import com.experienzia.infrastructure.persistence.entity.CertificadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificadoJpaRepository extends JpaRepository<CertificadoEntity, Long> {
    List<CertificadoEntity> findByUsuarioId(Long usuarioId);
    Optional<CertificadoEntity> findByCodigoUnico(String codigoUnico);
    Optional<CertificadoEntity> findByUsuarioIdAndEventoId(Long usuarioId, Long eventoId);
}
