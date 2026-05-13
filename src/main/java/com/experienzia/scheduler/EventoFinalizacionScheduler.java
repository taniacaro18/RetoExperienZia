package com.experienzia.scheduler;

import com.experienzia.service.EventoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pasa a {@code FINALIZADO} los eventos {@code ACTIVO} cuya fecha/hora de fin ya ocurrió.
 */
@Component
public class EventoFinalizacionScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(EventoFinalizacionScheduler.class);

    private final EventoService eventoService;

    public EventoFinalizacionScheduler(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    /** Cada 10 minutos (al minuto 0, 10, 20, …). */
    @Scheduled(cron = "0 */10 * * * *")
    public void finalizarEventosPasados() {
        try {
            eventoService.marcarEventosActivosFinalizados();
        } catch (Exception ex) {
            LOG.warn("Marcar eventos finalizados: {}", ex.getMessage());
        }
    }
}
