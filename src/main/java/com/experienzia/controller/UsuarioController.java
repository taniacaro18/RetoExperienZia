package com.experienzia.controller;

import com.experienzia.dto.ActualizarPerfilDTO;
import com.experienzia.dto.CrearStaffDTO;
import com.experienzia.dto.LoginDTO;
import com.experienzia.dto.LoginResponseDTO;
import com.experienzia.dto.RecuperarPasswordDTO;
import com.experienzia.dto.RecuperarPasswordResponseDTO;
import com.experienzia.dto.UsuarioDTO;
import com.experienzia.exceptions.CustomException;
import com.experienzia.security.JwtService;
import com.experienzia.service.AuditoriaService;
import com.experienzia.service.UsuarioService;
import com.experienzia.spec.UsuarioSpecification.UsuarioSearchCriteria;
import com.experienzia.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST de usuarios: registro, login, perfil y staff del organizador.
 * URL base: /api/usuarios
 * Lo usan todos los roles (login/registro) y el ORGANIZADOR (crear staff, reenviar credenciales).
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;
    private final JwtService jwtService;

    public UsuarioController(UsuarioService usuarioService, AuditoriaService auditoriaService,
                             JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.auditoriaService = auditoriaService;
        this.jwtService = jwtService;
    }

    // Registra un usuario nuevo (asistente u organizador). Devuelve UsuarioDTO.
    @PostMapping("/registro")
    public ResponseEntity<UsuarioDTO> registrar(@RequestBody UsuarioDTO dto) {
        return ResponseEntity.ok(usuarioService.registrar(dto));
    }

    // Inicia sesión con email y contraseña. Devuelve token JWT y datos del usuario.
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginDTO dto) {
        UsuarioDTO usuario = usuarioService.login(dto);
        String token = jwtService.generateToken(usuario.getId(), usuario.getEmail(), usuario.getRol());
        return ResponseEntity.ok(new LoginResponseDTO(token, usuario));
    }

    // El organizador crea una cuenta de staff. Devuelve UsuarioDTO del staff creado.
    @PostMapping("/staff")
    public ResponseEntity<UsuarioDTO> crearStaff(@RequestBody CrearStaffDTO dto) {
        return ResponseEntity.ok(usuarioService.crearStaff(dto));
    }

    // Lista todos los usuarios (normalmente admin). Devuelve lista de UsuarioDTO.
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    // Busca usuarios con filtros en la URL. Devuelve lista de UsuarioDTO.
    @GetMapping("/buscar")
    public ResponseEntity<List<UsuarioDTO>> buscar(UsuarioSearchCriteria criteria) {
        return ResponseEntity.ok(usuarioService.buscarPorCriterios(criteria));
    }

    // Obtiene un usuario por id. Devuelve UsuarioDTO.
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    // Actualiza nombre, teléfono u otros datos del perfil. Devuelve UsuarioDTO actualizado.
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> actualizarPerfil(@PathVariable Long id,
                                                       @RequestBody ActualizarPerfilDTO dto) {
        return ResponseEntity.ok(usuarioService.actualizarPerfil(id, dto));
    }

    // Pide recuperar contraseña por email. Devuelve mensaje de confirmación (sin mostrar la clave en claro).
    @PostMapping("/recuperar")
    public ResponseEntity<RecuperarPasswordResponseDTO> recuperar(@RequestBody RecuperarPasswordDTO dto) {
        return ResponseEntity.ok(usuarioService.recuperarPassword(dto));
    }

    // Reenvía credenciales a un asistente (organizador; el admin no puede usarlo). Devuelve RecuperarPasswordResponseDTO.
    @PostMapping("/{id}/reenviar-credenciales")
    public ResponseEntity<RecuperarPasswordResponseDTO> reenviarCredenciales(
            @PathVariable Long id,
            @RequestParam(required = false) Long actorId,
            HttpServletRequest request) {
        if (autenticadoEsAdmin()) {
            throw new CustomException(
                    "Los administradores no pueden restablecer contraseñas de otros usuarios.",
                    HttpStatus.FORBIDDEN);
        }
        RecuperarPasswordResponseDTO r = usuarioService.reenviarCredenciales(id);
        auditoriaService.registrar(actorId, "CREDENCIALES_REENVIADAS", "Usuario", id,
                ClientIpResolver.resolve(request));
        return ResponseEntity.ok(r);
    }

    // Revisa si quien está logueado tiene rol de administrador.
    private static boolean autenticadoEsAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
