package com.experienzia.repository;

import com.experienzia.entity.EstadoPago;
import com.experienzia.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para pagos de eventos.
 */
@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    /** Un evento tiene como maximo un pago asociado. */
    Optional<Pago> findByEventoId(Long eventoId);

    /** Pagos en un estado (PENDIENTE, APROBADO, RECHAZADO). */
    List<Pago> findByEstado(EstadoPago estado);

    /** Pagos de un organizador. */
    List<Pago> findByOrganizadorId(Long organizadorId);
}
