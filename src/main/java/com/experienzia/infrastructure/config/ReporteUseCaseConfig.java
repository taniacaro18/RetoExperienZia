package com.experienzia.infrastructure.config;

import com.experienzia.application.usecase.ReporteUseCase;
import com.experienzia.domain.port.ReporteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReporteUseCaseConfig {

    @Bean
    public ReporteUseCase reporteUseCase(ReporteRepository reporteRepository) {
        return new ReporteUseCase(reporteRepository);
    }
}
