package com.experienzia.domain.port;

import com.experienzia.domain.model.Pago;
import java.util.List;
import java.util.Optional;

public interface PagoRepository {
    Pago guardar(Pago pago);
    Optional<Pago> buscarPorId(Long id);
    Optional<Pago> buscarPorInscripcionId(Long inscripcionId);
    List<Pago> listarPendientes();
}
