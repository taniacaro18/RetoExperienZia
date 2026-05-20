package com.experienzia.controller;

import com.experienzia.dto.CambiarRolDTO;
import com.experienzia.dto.UsuarioDTO;
import com.experienzia.exceptions.CustomException;
import com.experienzia.service.AuditoriaService;
import com.experienzia.service.NotificacionService;
import com.experienzia.service.UsuarioService;
import com.experienzia.entity.TipoNotificacion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.experienzia.util.ClientIpResolver;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controlador REST de administración de usuarios (solo acciones de admin).
 * URL base: /api/admin/usuarios
 * Solo lo usa el rol ADMIN (aprobar organizadores, desactivar, cambiar rol, etc.).
 */
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;

    public AdminUsuarioController(UsuarioService usuarioService,
                                  NotificacionService notificacionService,
                                  AuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
    }

    // Aprueba la cuenta de un organizador pendiente. Devuelve UsuarioDTO actualizado.
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<UsuarioDTO> aprobarOrganizador(@PathVariable Long id,
                                                         @RequestParam(required = false) Long adminId,
                                                         HttpServletRequest request) {
        UsuarioDTO u = usuarioService.aprobarOrganizador(id);
        notificacionService.crear(u.getId(),
                "Tu cuenta de organizador fue aprobada. Ya puedes iniciar sesión.",
                TipoNotificacion.INFO);
        auditoriaService.registrar(adminId, "ORGANIZADOR_APROBADO", "Usuario", u.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(u);
    }

    // Rechaza la solicitud de organizador. Devuelve UsuarioDTO actualizado.
    @PutMapping("/{id}/rechazar")
    public ResponseEntity<UsuarioDTO> rechazarOrganizador(@PathVariable Long id,
                                                          @RequestParam(required = false) Long adminId,
                                                          HttpServletRequest request) {
        UsuarioDTO u = usuarioService.rechazarOrganizador(id);
        notificacionService.crear(u.getId(),
                "Tu solicitud de organizador fue rechazada por el administrador.",
                TipoNotificacion.ALERTA);
        auditoriaService.registrar(adminId, "ORGANIZADOR_RECHAZADO", "Usuario", u.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(u);
    }

    // Desactiva cualquier usuario del sistema. Devuelve UsuarioDTO actualizado.
    @PutMapping("/{id}/desactivar")
    public ResponseEntity<UsuarioDTO> desactivar(@PathVariable Long id,
                                                 @RequestParam(required = false) Long adminId,
                                                 HttpServletRequest request) {
        UsuarioDTO u = usuarioService.desactivar(id);
        notificacionService.crear(u.getId(),
                "Tu cuenta fue desactivada por el administrador.",
                TipoNotificacion.ALERTA);
        auditoriaService.registrar(adminId, "USUARIO_DESACTIVADO", "Usuario", u.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(u);
    }

    // Reactiva un usuario que estaba inactivo. Devuelve UsuarioDTO actualizado.
    @PutMapping("/{id}/reactivar")
    public ResponseEntity<UsuarioDTO> reactivar(@PathVariable Long id,
                                                @RequestParam(required = false) Long adminId,
                                                HttpServletRequest request) {
        UsuarioDTO u = usuarioService.reactivar(id);
        notificacionService.crear(u.getId(),
                "Tu cuenta fue reactivada por el administrador. Ya puedes iniciar sesión.",
                TipoNotificacion.INFO);
        auditoriaService.registrar(adminId, "USUARIO_REACTIVADO", "Usuario", u.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(u);
    }

    // Cambia el rol de un usuario (ADMIN, ORGANIZADOR, etc.). Devuelve UsuarioDTO actualizado.
    @PutMapping("/{id}/rol")
    public ResponseEntity<UsuarioDTO> cambiarRol(@PathVariable Long id,
                                                 @RequestBody CambiarRolDTO body,
                                                 @RequestParam(required = false) Long adminId,
                                                 HttpServletRequest request) {
        if (body == null || body.getRol() == null) {
            throw new CustomException("El rol es obligatorio.", HttpStatus.BAD_REQUEST);
        }
        UsuarioDTO u = usuarioService.cambiarRol(id, body.getRol());
        notificacionService.crear(u.getId(),
                "Tu rol fue actualizado por el administrador. Nuevo rol: " + u.getRol() + ".",
                TipoNotificacion.INFO);
        auditoriaService.registrar(adminId, "ROL_CAMBIADO_A_" + u.getRol(), "Usuario", u.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(u);
    }
}
