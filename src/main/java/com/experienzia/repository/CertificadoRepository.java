package com.experienzia.repository;

import com.experienzia.entity.Certificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para certificados de asistencia.
 */
@Repository
public interface CertificadoRepository extends JpaRepository<Certificado, Long> {

    /** Certificados de un usuario. */
    List<Certificado> findByUsuarioId(Long usuarioId);

    /** Certificados emitidos para un evento. */
    List<Certificado> findByEventoId(Long eventoId);

    /** Busca por codigo unico (validacion publica). */
    Optional<Certificado> findByCodigoUnico(String codigoUnico);

    /** Evita duplicar certificado para la misma asistencia. */
    Optional<Certificado> findByUsuarioIdAndEventoId(Long usuarioId, Long eventoId);
}
