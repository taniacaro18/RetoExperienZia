package com.experienzia.domain.port;

import com.experienzia.domain.model.Inscripcion;
import java.util.List;
import java.util.Optional;

public interface InscripcionRepository {
    Inscripcion guardar(Inscripcion inscripcion);
    Optional<Inscripcion> buscarPorId(Long id);
    Optional<Inscripcion> buscarPorUsuarioYEvento(Long usuarioId, Long eventoId);
    List<Inscripcion> listarPorEvento(Long eventoId);
}
