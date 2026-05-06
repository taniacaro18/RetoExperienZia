package com.experienzia.infrastructure.config;

import com.experienzia.application.usecase.CrearNotificacionUseCase;
import com.experienzia.application.usecase.ListarNotificacionesUseCase;
import com.experienzia.application.usecase.MarcarNotificacionLeidaUseCase;
import com.experienzia.domain.port.NotificacionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificacionUseCaseConfig {

    @Bean
    public CrearNotificacionUseCase crearNotificacionUseCase(NotificacionRepository notificacionRepository) {
        return new CrearNotificacionUseCase(notificacionRepository);
    }

    @Bean
    public ListarNotificacionesUseCase listarNotificacionesUseCase(NotificacionRepository notificacionRepository) {
        return new ListarNotificacionesUseCase(notificacionRepository);
    }

    @Bean
    public MarcarNotificacionLeidaUseCase marcarNotificacionLeidaUseCase(NotificacionRepository notificacionRepository) {
        return new MarcarNotificacionLeidaUseCase(notificacionRepository);
    }
}
