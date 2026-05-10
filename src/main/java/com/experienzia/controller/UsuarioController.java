package com.experienzia.controller;

import com.experienzia.dto.ActualizarPerfilDTO;
import com.experienzia.dto.CrearStaffDTO;
import com.experienzia.dto.LoginDTO;
import com.experienzia.dto.RecuperarPasswordDTO;
import com.experienzia.dto.RecuperarPasswordResponseDTO;
import com.experienzia.dto.UsuarioDTO;
import com.experienzia.service.AuditoriaService;
import com.experienzia.service.UsuarioService;
import com.experienzia.spec.UsuarioSpecification.UsuarioSearchCriteria;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    public UsuarioController(UsuarioService usuarioService, AuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioDTO> registrar(@RequestBody UsuarioDTO dto) {
        return ResponseEntity.ok(usuarioService.registrar(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<UsuarioDTO> login(@RequestBody LoginDTO dto) {
        return ResponseEntity.ok(usuarioService.login(dto));
    }

    @PostMapping("/staff")
    public ResponseEntity<UsuarioDTO> crearStaff(@RequestBody CrearStaffDTO dto) {
        return ResponseEntity.ok(usuarioService.crearStaff(dto));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<UsuarioDTO>> buscar(UsuarioSearchCriteria criteria) {
        return ResponseEntity.ok(usuarioService.buscarPorCriterios(criteria));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> actualizarPerfil(@PathVariable Long id,
                                                       @RequestBody ActualizarPerfilDTO dto) {
        return ResponseEntity.ok(usuarioService.actualizarPerfil(id, dto));
    }

    @PostMapping("/recuperar")
    public ResponseEntity<RecuperarPasswordResponseDTO> recuperar(@RequestBody RecuperarPasswordDTO dto) {
        return ResponseEntity.ok(usuarioService.recuperarPassword(dto));
    }

    /**
     * Reenviar credenciales de un usuario (acción del organizador/admin
     * para asistentes cargados masivamente que olvidaron su contraseña inicial).
     * Resetea la contraseña al número de documento (o genera una temporal) y notifica al usuario.
     */
    @PostMapping("/{id}/reenviar-credenciales")
    public ResponseEntity<RecuperarPasswordResponseDTO> reenviarCredenciales(
            @PathVariable Long id,
            @RequestParam(required = false) Long actorId) {
        RecuperarPasswordResponseDTO r = usuarioService.reenviarCredenciales(id);
        auditoriaService.registrar(actorId, "CREDENCIALES_REENVIADAS", "Usuario", id);
        return ResponseEntity.ok(r);
    }
}
