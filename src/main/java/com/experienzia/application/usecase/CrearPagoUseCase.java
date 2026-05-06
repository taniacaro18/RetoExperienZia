package com.experienzia.application.usecase;

import com.experienzia.domain.model.EstadoPago;
import com.experienzia.domain.model.Pago;
import com.experienzia.domain.port.InscripcionRepository;
import com.experienzia.domain.port.PagoRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public class CrearPagoUseCase {

    private final PagoRepository pagoRepository;
    private final InscripcionRepository inscripcionRepository;

    public CrearPagoUseCase(PagoRepository pagoRepository, InscripcionRepository inscripcionRepository) {
        this.pagoRepository = pagoRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    public Pago ejecutar(Long inscripcionId, String comprobanteUrl) {
        if (inscripcionRepository.buscarPorId(inscripcionId).isEmpty()) {
            throw new IllegalArgumentException("La inscripción no existe.");
        }

        Optional<Pago> pagoExistente = pagoRepository.buscarPorInscripcionId(inscripcionId);
        if (pagoExistente.isPresent()) {
            EstadoPago estadoActual = pagoExistente.get().getEstado();
            if (estadoActual == EstadoPago.APROBADO || estadoActual == EstadoPago.PENDIENTE) {
                throw new IllegalStateException("Ya existe un pago activo (PENDIENTE o APROBADO) para esta inscripción.");
            }
        }

        Pago nuevoPago = new Pago();
        nuevoPago.setInscripcionId(inscripcionId);
        nuevoPago.setComprobanteUrl(comprobanteUrl);
        nuevoPago.setEstado(EstadoPago.PENDIENTE);
        nuevoPago.setFecha(LocalDateTime.now());

        return pagoRepository.guardar(nuevoPago);
    }
}
