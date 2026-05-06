package com.experienzia.application.usecase;

import com.experienzia.domain.model.Pago;
import com.experienzia.domain.port.PagoRepository;

public class AprobarPagoUseCase {

    private final PagoRepository pagoRepository;

    public AprobarPagoUseCase(PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
    }

    public Pago ejecutar(Long pagoId) {
        Pago pago = pagoRepository.buscarPorId(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("El pago no existe."));
        
        pago.aprobar();
        return pagoRepository.guardar(pago);
    }
}
