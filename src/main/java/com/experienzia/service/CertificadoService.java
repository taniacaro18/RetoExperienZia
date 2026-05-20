package com.experienzia.service;

import com.experienzia.dto.CertificadoDTO;

import java.util.List;

/**
 * Contrato para generar certificados de asistencia.
 */
public interface CertificadoService {

    /** Genera un certificado para una inscripcion que ya asistio. */
    CertificadoDTO generar(Long inscripcionId);
    /** HU-024: generación masiva para todos los asistentes confirmados de un evento. */
    List<CertificadoDTO> generarMasivoPorEvento(Long eventoId, Long organizadorId);
    /** Certificados de un usuario. */
    List<CertificadoDTO> listarPorUsuario(Long usuarioId);

    /** Certificados emitidos para un evento. */
    List<CertificadoDTO> listarPorEvento(Long eventoId);

    /** Valida un certificado por su codigo unico (endpoint publico). */
    CertificadoDTO validarPorCodigo(String codigoUnico);
}
