package com.experienzia.adapters.outbound;

import com.experienzia.domain.model.Evento;
import com.experienzia.domain.port.EventoRepository;
import com.experienzia.infrastructure.persistence.EventoJpaRepository;
import com.experienzia.infrastructure.persistence.entity.EventoEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class EventoRepositoryImpl implements EventoRepository {

    private final EventoJpaRepository eventoJpaRepository;

    public EventoRepositoryImpl(EventoJpaRepository eventoJpaRepository) {
        this.eventoJpaRepository = eventoJpaRepository;
    }

    @Override
    public Evento guardar(Evento evento) {
        EventoEntity entity = toEntity(evento);
        EventoEntity savedEntity = eventoJpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Evento> buscarPorId(Long id) {
        return eventoJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Evento> listarTodos() {
        return eventoJpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void eliminar(Long id) {
        eventoJpaRepository.deleteById(id);
    }

    // --- Mappers privados ---

    private EventoEntity toEntity(Evento evento) {
        if (evento == null) {
            return null;
        }
        return new EventoEntity(
                evento.getId(),
                evento.getNombre(),
                evento.getDescripcion(),
                evento.getFecha(),
                evento.getUbicacion(),
                evento.getTipoEvento(),
                evento.getEstado(),
                evento.getAforoMaximo(),
                evento.getAforoActual(),
                evento.getCosto(),
                evento.getOrganizadorId(),
                evento.getImagen()
        );
    }

    private Evento toDomain(EventoEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Evento(
                entity.getId(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getFecha(),
                entity.getUbicacion(),
                entity.getTipoEvento(),
                entity.getEstado(),
                entity.getAforoMaximo(),
                entity.getAforoActual(),
                entity.getCosto(),
                entity.getOrganizadorId(),
                entity.getImagen()
        );
    }
}
