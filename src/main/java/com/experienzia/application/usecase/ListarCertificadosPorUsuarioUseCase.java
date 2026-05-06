package com.experienzia.application.usecase;

import com.experienzia.domain.model.Certificado;
import com.experienzia.domain.port.CertificadoRepository;
import java.util.List;

public class ListarCertificadosPorUsuarioUseCase {

    private final CertificadoRepository certificadoRepository;

    public ListarCertificadosPorUsuarioUseCase(CertificadoRepository certificadoRepository) {
        this.certificadoRepository = certificadoRepository;
    }

    public List<Certificado> ejecutar(Long usuarioId) {
        return certificadoRepository.listarPorUsuario(usuarioId);
    }
}
