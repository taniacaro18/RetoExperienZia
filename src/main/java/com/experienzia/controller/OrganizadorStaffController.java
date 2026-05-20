package com.experienzia.controller;

import com.experienzia.dto.UsuarioDTO;
import com.experienzia.entity.TipoNotificacion;
import com.experienzia.service.AuditoriaService;
import com.experienzia.service.NotificacionService;
import com.experienzia.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import com.experienzia.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para que el organizador gestione a su personal (staff).
 * URL base: /api/organizadores/{organizadorId}/staff
 * Solo lo usa el rol ORGANIZADOR (desactivar o reactivar cuentas de staff que él creó).
 */
@RestController
@RequestMapping("/api/organizadores/{organizadorId}/staff")
public class OrganizadorStaffController {

    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;

    public OrganizadorStaffController(UsuarioService usuarioService,
                                      NotificacionService notificacionService,
                                      AuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
    }

    // Desactiva la cuenta de un staff del organizador. Devuelve el usuario actualizado.
    @PutMapping("/{staffId}/desactivar")
    public ResponseEntity<UsuarioDTO> desactivar(@PathVariable Long organizadorId,
                                                 @PathVariable Long staffId,
                                                 HttpServletRequest request) {
        UsuarioDTO u = usuarioService.desactivarStaffPorOrganizador(organizadorId, staffId);
        notificacionService.crear(u.getId(),
                "El organizador desactivó tu cuenta de staff. Ya no podrás iniciar sesión hasta que sea reactivada.",
                TipoNotificacion.ALERTA);
        auditoriaService.registrar(organizadorId, "STAFF_DESACTIVADO", "Usuario", u.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(u);
    }

    // Reactiva la cuenta de un staff que estaba desactivado. Devuelve el usuario actualizado.
    @PutMapping("/{staffId}/reactivar")
    public ResponseEntity<UsuarioDTO> reactivar(@PathVariable Long organizadorId,
                                                @PathVariable Long staffId,
                                                HttpServletRequest request) {
        UsuarioDTO u = usuarioService.reactivarStaffPorOrganizador(organizadorId, staffId);
        notificacionService.crear(u.getId(),
                "El organizador reactivó tu cuenta de staff. Ya puedes iniciar sesión nuevamente.",
                TipoNotificacion.INFO);
        auditoriaService.registrar(organizadorId, "STAFF_REACTIVADO", "Usuario", u.getId(),
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(u);
    }
}
