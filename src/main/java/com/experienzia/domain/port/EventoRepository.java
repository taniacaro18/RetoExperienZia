package com.experienzia.domain.port;

import com.experienzia.domain.model.Evento;
import java.util.List;
import java.util.Optional;

public interface EventoRepository {
    
    Evento guardar(Evento evento);
    
    Optional<Evento> buscarPorId(Long id);
    
    List<Evento> listarTodos();
    
    void eliminar(Long id);
}
