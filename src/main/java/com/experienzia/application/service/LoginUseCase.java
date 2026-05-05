package com.experienzia.application.service;

import com.experienzia.domain.exception.CredencialesInvalidasException;
import com.experienzia.domain.exception.UsuarioNoAutorizadoException;
import com.experienzia.domain.exception.UsuarioNoEncontradoException;
import com.experienzia.domain.model.Estado;
import com.experienzia.domain.model.Usuario;
import com.experienzia.domain.port.UsuarioRepository;
import java.util.Optional;

public class LoginUseCase {

    private final UsuarioRepository usuarioRepository;

    public LoginUseCase(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * HU-003: Inicio de Sesión
     */
    public Usuario login(String email, String password) {
        // 1. Buscar usuario por email
        Optional<Usuario> usuarioOpt = usuarioRepository.buscarPorEmail(email);

        if (usuarioOpt.isEmpty()) {
            throw new UsuarioNoEncontradoException("No existe una cuenta registrada con este correo.");
        }

        Usuario usuario = usuarioOpt.get();

        // 2. Validar contraseña
        if (!usuario.getPassword().equals(password)) {
            throw new CredencialesInvalidasException("La contraseña ingresada es incorrecta.");
        }

        // 3. Verificar estado del usuario
        if (usuario.getEstado() == Estado.PENDIENTE || 
            usuario.getEstado() == Estado.RECHAZADO || 
            usuario.getEstado() == Estado.INACTIVO) {
            throw new UsuarioNoAutorizadoException("Acceso denegado. El estado de la cuenta es: " + usuario.getEstado());
        }

        // 4. Retornar el usuario si todo es válido
        return usuario;
    }
}
