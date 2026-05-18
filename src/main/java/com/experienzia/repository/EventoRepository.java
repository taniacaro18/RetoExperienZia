package com.experienzia.repository;

import com.experienzia.entity.EstadoEvento;
import com.experienzia.entity.Evento;
import com.experienzia.entity.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long>, JpaSpecificationExecutor<Evento> {
    List<Evento> findByTipoEventoAndEstado(TipoEvento tipoEvento, EstadoEvento estado);

    List<Evento> findByEstado(EstadoEvento estado);

    List<Evento> findByOrganizadorId(Long organizadorId);

    @Query("SELECT DISTINCT e FROM Evento e LEFT JOIN FETCH e.organizador WHERE e.id = :id")
    Optional<Evento> findByIdWithOrganizador(@Param("id") Long id);

    /** Eventos que reservan una ubicación (misma sala, comparación sin distinguir mayúsculas). */
    @Query("""
            SELECT e FROM Evento e
            WHERE e.estado IN :estados
            AND LOWER(TRIM(COALESCE(e.ubicacion, ''))) = LOWER(TRIM(:ubicacion))
            """)
    List<Evento> findByUbicacionNormalizadaYEstadoIn(
            @Param("ubicacion") String ubicacion,
            @Param("estados") Collection<EstadoEvento> estados);
}
