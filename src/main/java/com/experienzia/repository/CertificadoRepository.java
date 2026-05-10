package com.experienzia.repository;

import com.experienzia.entity.Certificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificadoRepository extends JpaRepository<Certificado, Long> {
    List<Certificado> findByUsuarioId(Long usuarioId);
    List<Certificado> findByEventoId(Long eventoId);
    Optional<Certificado> findByCodigoUnico(String codigoUnico);
    Optional<Certificado> findByUsuarioIdAndEventoId(Long usuarioId, Long eventoId);
}
