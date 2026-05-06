package com.experienzia.application.service;

import com.experienzia.domain.exception.DatosInvalidosException;
import com.experienzia.domain.exception.UsuarioNoAutorizadoException;
import com.experienzia.domain.exception.UsuarioNoEncontradoException;
import com.experienzia.domain.exception.UsuarioYaExisteException;
import com.experienzia.domain.model.Rol;
import com.experienzia.domain.model.Usuario;
import com.experienzia.domain.port.UsuarioRepository;

/**
 * HU-004: Creación de STAFF por un Organizador.
 * <p>
 * Reglas:
 * - El usuario actual debe tener rol ORGANIZADOR.
 * - El STAFF queda asociado al organizador que lo creó (organizadorId).
 * - Un STAFF no puede registrarse de forma independiente.
 * - Se valida email único y datos obligatorios.
 */
public class CrearStaffUseCase {

    private final UsuarioRepository usuarioRepository;

    public CrearStaffUseCase(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Crea un usuario con rol STAFF vinculado al organizador indicado.
     *
     * @param nuevoStaff    datos del nuevo usuario STAFF
     * @param organizadorId ID del organizador que crea al STAFF
     * @return Usuario STAFF creado y activo
     */
    public Usuario crearStaff(Usuario nuevoStaff, Long organizadorId) {
        // 1. Verificar que el organizador existe y tiene el rol correcto
        Usuario organizador = usuarioRepository.buscarPorId(organizadorId)
                .orElseThrow(() -> new UsuarioNoEncontradoException(
                        "No se encontró el organizador con ID: " + organizadorId));

        if (organizador.getRol() != Rol.ORGANIZADOR) {
            throw new UsuarioNoAutorizadoException(
                    "Solo un ORGANIZADOR puede crear usuarios STAFF.");
        }

        // 2. Validar datos del nuevo STAFF
        validarDatosObligatorios(nuevoStaff);
        validarEmailUnico(nuevoStaff.getEmail());

        // 3. Asignar rol STAFF y vincular al organizador
        nuevoStaff.asignarRolStaff(organizadorId);

        return usuarioRepository.guardar(nuevoStaff);
    }

    // ---------------------------------------------------------------
    // Validaciones privadas
    // ---------------------------------------------------------------

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
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (!usuario.getEmail().matches(emailRegex)) {
            throw new DatosInvalidosException("El formato del correo electrónico no es válido.");
        }
    }

    private void validarEmailUnico(String email) {
        if (usuarioRepository.existePorEmail(email)) {
            throw new UsuarioYaExisteException(
                    "El correo electrónico ya se encuentra registrado.");
        }
    }
}
