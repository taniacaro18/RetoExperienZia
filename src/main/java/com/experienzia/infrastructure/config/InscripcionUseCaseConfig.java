package com.experienzia.infrastructure.config;

import com.experienzia.application.usecase.CancelarInscripcionUseCase;
import com.experienzia.application.usecase.CrearInscripcionUseCase;
import com.experienzia.application.usecase.ListarInscripcionesPorEventoUseCase;
import com.experienzia.domain.port.EventoRepository;
import com.experienzia.domain.port.InscripcionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InscripcionUseCaseConfig {

    @Bean
    public CrearInscripcionUseCase crearInscripcionUseCase(InscripcionRepository inscripcionRepository, EventoRepository eventoRepository) {
        return new CrearInscripcionUseCase(inscripcionRepository, eventoRepository);
    }

    @Bean
    public CancelarInscripcionUseCase cancelarInscripcionUseCase(InscripcionRepository inscripcionRepository, EventoRepository eventoRepository) {
        return new CancelarInscripcionUseCase(inscripcionRepository, eventoRepository);
    }

    @Bean
    public ListarInscripcionesPorEventoUseCase listarInscripcionesPorEventoUseCase(InscripcionRepository inscripcionRepository) {
        return new ListarInscripcionesPorEventoUseCase(inscripcionRepository);
    }
}
