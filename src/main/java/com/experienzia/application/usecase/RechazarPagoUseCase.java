package com.experienzia.application.usecase;

import com.experienzia.domain.model.Pago;
import com.experienzia.domain.port.PagoRepository;

public class RechazarPagoUseCase {

    private final PagoRepository pagoRepository;

    public RechazarPagoUseCase(PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
    }

    public Pago ejecutar(Long pagoId) {
        Pago pago = pagoRepository.buscarPorId(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("El pago no existe."));
        
        pago.rechazar();
        return pagoRepository.guardar(pago);
    }
}
