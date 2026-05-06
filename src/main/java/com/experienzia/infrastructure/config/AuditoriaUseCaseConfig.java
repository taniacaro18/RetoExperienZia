package com.experienzia.infrastructure.config;

import com.experienzia.application.usecase.CrearAuditoriaUseCase;
import com.experienzia.application.usecase.ListarAuditoriasUseCase;
import com.experienzia.domain.port.AuditoriaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditoriaUseCaseConfig {

    @Bean
    public CrearAuditoriaUseCase crearAuditoriaUseCase(AuditoriaRepository auditoriaRepository) {
        return new CrearAuditoriaUseCase(auditoriaRepository);
    }

    @Bean
    public ListarAuditoriasUseCase listarAuditoriasUseCase(AuditoriaRepository auditoriaRepository) {
        return new ListarAuditoriasUseCase(auditoriaRepository);
    }
}
