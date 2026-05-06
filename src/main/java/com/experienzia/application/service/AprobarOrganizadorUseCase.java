package com.experienzia.application.service;

import com.experienzia.domain.exception.DatosInvalidosException;
import com.experienzia.domain.exception.UsuarioNoEncontradoException;
import com.experienzia.domain.model.Estado;
import com.experienzia.domain.model.Rol;
import com.experienzia.domain.model.Usuario;
import com.experienzia.domain.port.UsuarioRepository;

/**
 * HU-002a: Aprobación de Organizador por el Administrador.
 * Solo usuarios con rol ORGANIZADOR en estado PENDIENTE pueden ser aprobados.
 */
public class AprobarOrganizadorUseCase {

    private final UsuarioRepository usuarioRepository;

    public AprobarOrganizadorUseCase(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Aprueba la solicitud de registro de un organizador.
     *
     * @param id ID del usuario a aprobar
     * @return Usuario actualizado con estado ACTIVO
     */
    public Usuario aprobar(Long id) {
        Usuario usuario = usuarioRepository.buscarPorId(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(
                        "No se encontró un usuario con el ID: " + id));

        // Solo se pueden aprobar organizadores pendientes
        if (usuario.getRol() != Rol.ORGANIZADOR) {
            throw new DatosInvalidosException(
                    "Solo se pueden aprobar usuarios con rol ORGANIZADOR.");
        }
        if (usuario.getEstado() != Estado.PENDIENTE) {
            throw new DatosInvalidosException(
                    "El usuario no está en estado PENDIENTE. Estado actual: " + usuario.getEstado());
        }

        usuario.activarUsuario();
        return usuarioRepository.guardar(usuario);
    }
}
