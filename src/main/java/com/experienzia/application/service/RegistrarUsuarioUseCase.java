package com.experienzia.application.service;

import com.experienzia.domain.exception.DatosInvalidosException;
import com.experienzia.domain.exception.UsuarioNoAutorizadoException;
import com.experienzia.domain.exception.UsuarioYaExisteException;
import com.experienzia.domain.model.Usuario;
import com.experienzia.domain.port.UsuarioRepository;

public class RegistrarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;

    public RegistrarUsuarioUseCase(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * HU-001: Registro de asistentes.
     * El estado se establece automáticamente en ACTIVO.
     */
    public Usuario registrarAsistente(Usuario usuario) {
        validarDatosObligatorios(usuario);
        validarEmailUnico(usuario.getEmail());
        // Regla: solo ASISTENTE y ORGANIZADOR se registran públicamente
        validarRolNoRestringido(usuario);

        usuario.asignarRolAsistente();

        return usuarioRepository.guardar(usuario);
    }

    /**
     * HU-002: Solicitud de Registro de Organizadores.
     * El estado queda en PENDIENTE hasta aprobación del ADMIN.
     */
    public Usuario registrarOrganizador(Usuario usuario) {
        validarDatosObligatorios(usuario);
        validarEmailUnico(usuario.getEmail());
        // Regla: solo ASISTENTE y ORGANIZADOR se registran públicamente
        validarRolNoRestringido(usuario);

        usuario.marcarOrganizadorPendiente();

        return usuarioRepository.guardar(usuario);
    }

    // ---------------------------------------------------------------
    // Validaciones privadas
    // ---------------------------------------------------------------

    /**
     * Impide que desde el registro público se solicite el rol ADMIN o STAFF.
     * El rol aún no está asignado en este punto, así que protegemos contra
     * intentos de manipulación del campo "tipo" en el DTO.
     */
    private void validarRolNoRestringido(Usuario usuario) {
        if (usuario.getRol() != null) {
            switch (usuario.getRol()) {
                case ADMIN:
                    throw new UsuarioNoAutorizadoException(
                            "No se permite registrar un usuario con rol ADMIN desde el registro público.");
                case STAFF:
                    throw new UsuarioNoAutorizadoException(
                            "El rol STAFF solo puede ser asignado por un Organizador. " +
                            "No puede registrarse de forma independiente.");
                default:
                    break;
            }
        }
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
