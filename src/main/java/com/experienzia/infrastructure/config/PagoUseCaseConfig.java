package com.experienzia.infrastructure.config;

import com.experienzia.application.usecase.AprobarPagoUseCase;
import com.experienzia.application.usecase.CrearPagoUseCase;
import com.experienzia.application.usecase.RechazarPagoUseCase;
import com.experienzia.domain.port.InscripcionRepository;
import com.experienzia.domain.port.PagoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PagoUseCaseConfig {

    @Bean
    public CrearPagoUseCase crearPagoUseCase(PagoRepository pagoRepository, InscripcionRepository inscripcionRepository) {
        return new CrearPagoUseCase(pagoRepository, inscripcionRepository);
    }

    @Bean
    public AprobarPagoUseCase aprobarPagoUseCase(PagoRepository pagoRepository) {
        return new AprobarPagoUseCase(pagoRepository);
    }

    @Bean
    public RechazarPagoUseCase rechazarPagoUseCase(PagoRepository pagoRepository) {
        return new RechazarPagoUseCase(pagoRepository);
    }
}
