package com.experienzia.application.service;

import com.experienzia.domain.model.Usuario;
import com.experienzia.domain.port.UsuarioRepository;
import java.util.List;

public class ListarUsuariosUseCase {

    private final UsuarioRepository usuarioRepository;

    public ListarUsuariosUseCase(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.listarTodos();
    }
}
