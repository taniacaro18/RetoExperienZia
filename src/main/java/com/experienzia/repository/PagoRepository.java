package com.experienzia.repository;

import com.experienzia.entity.EstadoPago;
import com.experienzia.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByEventoId(Long eventoId);
    List<Pago> findByEstado(EstadoPago estado);
    List<Pago> findByOrganizadorId(Long organizadorId);
}
