package com.experienzia.service;

import com.experienzia.dto.ActualizarPerfilDTO;
import com.experienzia.dto.CrearStaffDTO;
import com.experienzia.dto.LoginDTO;
import com.experienzia.dto.RecuperarPasswordDTO;
import com.experienzia.dto.RecuperarPasswordResponseDTO;
import com.experienzia.dto.UsuarioDTO;
import com.experienzia.spec.UsuarioSpecification.UsuarioSearchCriteria;

import java.util.List;

public interface UsuarioService {

    /** HU-001 / HU-002: registro público. dto.tipo decide ASISTENTE u ORGANIZADOR. */
    UsuarioDTO registrar(UsuarioDTO dto);

    /** HU-003: inicio de sesión. */
    UsuarioDTO login(LoginDTO dto);

    /** HU-004: creación de STAFF por un organizador. */
    UsuarioDTO crearStaff(CrearStaffDTO dto);

    /** HU-002a: el admin aprueba un organizador pendiente. */
    UsuarioDTO aprobarOrganizador(Long id);

    /** HU-002b: el admin rechaza un organizador pendiente. */
    UsuarioDTO rechazarOrganizador(Long id);

    /** El admin desactiva una cuenta. */
    UsuarioDTO desactivar(Long id);

    /** HU-019: el admin reactiva una cuenta INACTIVA. */
    UsuarioDTO reactivar(Long id);

    /** HU-004: el organizador desactiva la cuenta de un STAFF que él creó. */
    UsuarioDTO desactivarStaffPorOrganizador(Long organizadorId, Long staffId);

    /** HU-004: el organizador reactiva la cuenta de un STAFF que él creó. */
    UsuarioDTO reactivarStaffPorOrganizador(Long organizadorId, Long staffId);

    /** HU-018: el admin cambia el rol y/o el estado de un usuario. */
    UsuarioDTO cambiarRol(Long id, String nuevoRol);

    /** HU-005: ver perfil. */
    UsuarioDTO obtenerPorId(Long id);

    /** HU-005: editar perfil (solo teléfono y contraseña). */
    UsuarioDTO actualizarPerfil(Long id, ActualizarPerfilDTO dto);

    /** HU-006: recuperación de contraseña sin correo (devuelve la temporal). */
    RecuperarPasswordResponseDTO recuperarPassword(RecuperarPasswordDTO dto);

    /**
     * Reenvía las credenciales iniciales de un asistente cargado masivamente:
     * resetea su contraseña al número de documento (o genera una temporal si no tiene)
     * y notifica al usuario por el sistema.
     */
    RecuperarPasswordResponseDTO reenviarCredenciales(Long usuarioId);

    /** Listado completo (admin). */
    List<UsuarioDTO> listarTodos();

    /** Búsqueda por criterios libres (nombre, email, rol, estado, organizadorId). */
    List<UsuarioDTO> buscarPorCriterios(UsuarioSearchCriteria criteria);
}
