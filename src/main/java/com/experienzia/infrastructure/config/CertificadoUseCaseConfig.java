package com.experienzia.infrastructure.config;

import com.experienzia.application.usecase.GenerarCertificadoUseCase;
import com.experienzia.application.usecase.ListarCertificadosPorUsuarioUseCase;
import com.experienzia.application.usecase.ValidarCertificadoUseCase;
import com.experienzia.domain.port.CertificadoRepository;
import com.experienzia.domain.port.InscripcionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CertificadoUseCaseConfig {

    @Bean
    public GenerarCertificadoUseCase generarCertificadoUseCase(CertificadoRepository certificadoRepository, InscripcionRepository inscripcionRepository) {
        return new GenerarCertificadoUseCase(certificadoRepository, inscripcionRepository);
    }

    @Bean
    public ListarCertificadosPorUsuarioUseCase listarCertificadosPorUsuarioUseCase(CertificadoRepository certificadoRepository) {
        return new ListarCertificadosPorUsuarioUseCase(certificadoRepository);
    }

    @Bean
    public ValidarCertificadoUseCase validarCertificadoUseCase(CertificadoRepository certificadoRepository) {
        return new ValidarCertificadoUseCase(certificadoRepository);
    }
}
