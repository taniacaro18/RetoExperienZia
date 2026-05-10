package com.experienzia.service;

import com.experienzia.dto.CertificadoDTO;

import java.util.List;

public interface CertificadoService {
    CertificadoDTO generar(Long inscripcionId);
    /** HU-024: generación masiva para todos los asistentes confirmados de un evento. */
    List<CertificadoDTO> generarMasivoPorEvento(Long eventoId, Long organizadorId);
    List<CertificadoDTO> listarPorUsuario(Long usuarioId);
    List<CertificadoDTO> listarPorEvento(Long eventoId);
    CertificadoDTO validarPorCodigo(String codigoUnico);
}
