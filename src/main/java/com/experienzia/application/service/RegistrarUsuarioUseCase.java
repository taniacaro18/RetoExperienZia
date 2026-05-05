package com.experienzia.application.service;

import com.experienzia.domain.exception.DatosInvalidosException;
import com.experienzia.domain.exception.UsuarioYaExisteException;
import com.experienzia.domain.model.Usuario;
import com.experienzia.domain.port.UsuarioRepository;

public class RegistrarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;

    public RegistrarUsuarioUseCase(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * HU-001: Registro de asistentes
     */
    public Usuario registrarAsistente(Usuario usuario) {
        validarDatosObligatorios(usuario);
        validarEmailUnico(usuario.getEmail());
        
        usuario.asignarRolAsistente();
        
        return usuarioRepository.guardar(usuario);
    }

    /**
     * HU-002: Solicitud Registro de Organizadores
     */
    public Usuario registrarOrganizador(Usuario usuario) {
        validarDatosObligatorios(usuario);
        validarEmailUnico(usuario.getEmail());
        
        usuario.marcarOrganizadorPendiente();
        
        return usuarioRepository.guardar(usuario);
    }

    /**
     * Validación de campos obligatorios y formato. (Sin usar anotaciones Spring)
     */
    private void validarDatosObligatorios(Usuario usuario) {
        if (usuario.getNombre() == null || usuario.getNombre().trim().isEmpty()) {
            throw new DatosInvalidosException("El nombre es un campo obligatorio.");
        }
        
        if (usuario.getPassword() == null || usuario.getPassword().trim().isEmpty()) {
            throw new DatosInvalidosException("La contraseña es un campo obligatorio.");
        }
        
        if (usuario.getEmail() == null || usuario.getEmail().trim().isEmpty()) {
            throw new DatosInvalidosException("El correo electrónico es un campo obligatorio.");
        }
        
        // Formato básico de correo electrónico usando Expresión Regular (Regex)
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (!usuario.getEmail().matches(emailRegex)) {
            throw new DatosInvalidosException("El formato del correo electrónico no es válido.");
        }
    }

    /**
     * Valida que el email no esté duplicado en el sistema.
     */
    private void validarEmailUnico(String email) {
        if (usuarioRepository.existePorEmail(email)) {
            throw new UsuarioYaExisteException("El correo electrónico ya se encuentra registrado.");
        }
    }
}
