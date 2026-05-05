package com.experienzia.domain.port;

import com.experienzia.domain.model.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {
    
    Usuario guardar(Usuario usuario);
    
    Optional<Usuario> buscarPorEmail(String email);
    
    boolean existePorEmail(String email);
    
    List<Usuario> listarTodos();
    
    void eliminar(Long id);
}
