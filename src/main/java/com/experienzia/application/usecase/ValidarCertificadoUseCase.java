package com.experienzia.application.usecase;

import com.experienzia.domain.model.Certificado;
import com.experienzia.domain.port.CertificadoRepository;

public class ValidarCertificadoUseCase {

    private final CertificadoRepository certificadoRepository;

    public ValidarCertificadoUseCase(CertificadoRepository certificadoRepository) {
        this.certificadoRepository = certificadoRepository;
    }

    public Certificado ejecutar(String codigo) {
        return certificadoRepository.buscarPorCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("El código proporcionado no corresponde a ningún certificado válido."));
    }
}
