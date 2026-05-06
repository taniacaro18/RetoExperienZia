package com.experienzia.application.usecase;

import com.experienzia.domain.model.Certificado;
import com.experienzia.domain.model.EstadoInscripcion;
import com.experienzia.domain.model.Inscripcion;
import com.experienzia.domain.port.CertificadoRepository;
import com.experienzia.domain.port.InscripcionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class GenerarCertificadoUseCase {

    private final CertificadoRepository certificadoRepository;
    private final InscripcionRepository inscripcionRepository;

    public GenerarCertificadoUseCase(CertificadoRepository certificadoRepository, InscripcionRepository inscripcionRepository) {
        this.certificadoRepository = certificadoRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    public Certificado ejecutar(Long inscripcionId) {
        Inscripcion inscripcion = inscripcionRepository.buscarPorId(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada."));

        if (inscripcion.getEstado() != EstadoInscripcion.ASISTIO) {
            throw new IllegalStateException("El certificado solo puede generarse para usuarios que hayan asistido al evento.");
        }

        Optional<Certificado> existente = certificadoRepository.buscarPorUsuarioYEvento(inscripcion.getUsuarioId(), inscripcion.getEventoId());
        if (existente.isPresent()) {
            throw new IllegalStateException("Ya existe un certificado generado para esta asistencia.");
        }

        Certificado certificado = new Certificado();
        certificado.setUsuarioId(inscripcion.getUsuarioId());
        certificado.setEventoId(inscripcion.getEventoId());
        certificado.setFechaGeneracion(LocalDateTime.now());
        certificado.setCodigoUnico(UUID.randomUUID().toString());

        return certificadoRepository.guardar(certificado);
    }
}
