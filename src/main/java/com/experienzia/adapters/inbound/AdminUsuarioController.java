package com.experienzia.adapters.inbound;

import com.experienzia.adapters.dto.UsuarioDtoMapper;
import com.experienzia.adapters.dto.UsuarioResponseDTO;
import com.experienzia.application.service.AprobarOrganizadorUseCase;
import com.experienzia.application.service.RechazarOrganizadorUseCase;
import com.experienzia.domain.exception.DatosInvalidosException;
import com.experienzia.domain.exception.UsuarioNoAutorizadoException;
import com.experienzia.domain.exception.UsuarioNoEncontradoException;
import com.experienzia.domain.model.Usuario;
import com.experienzia.domain.port.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints exclusivos para el ADMINISTRADOR.
 * <p>
 * En producción estos endpoints deben estar protegidos con Spring Security
 * (p. ej. @PreAuthorize("hasRole('ADMIN')")). Aquí la protección de rol
 * queda como placeholder comentado para no introducir dependencias adicionales.
 *
 * Endpoints:
 * PUT /api/admin/usuarios/{id}/aprobar   → Aprueba un organizador pendiente
 * PUT /api/admin/usuarios/{id}/rechazar  → Rechaza un organizador pendiente
 */
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {

    private final AprobarOrganizadorUseCase aprobarOrganizadorUseCase;
    private final RechazarOrganizadorUseCase rechazarOrganizadorUseCase;

    public AdminUsuarioController(UsuarioRepository usuarioRepository) {
        this.aprobarOrganizadorUseCase = new AprobarOrganizadorUseCase(usuarioRepository);
        this.rechazarOrganizadorUseCase = new RechazarOrganizadorUseCase(usuarioRepository);
    }

    /**
     * Aprueba la solicitud de registro de un ORGANIZADOR.
     * Cambia su estado de PENDIENTE → ACTIVO.
     *
     * @param id ID del usuario organizador a aprobar
     */
    // @PreAuthorize("hasRole('ADMIN')")  ← activar con Spring Security
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobarOrganizador(@PathVariable Long id) {
        try {
            Usuario aprobado = aprobarOrganizadorUseCase.aprobar(id);
            UsuarioResponseDTO responseDTO = UsuarioDtoMapper.toDto(aprobado);
            return ResponseEntity.ok(responseDTO);

        } catch (UsuarioNoEncontradoException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (DatosInvalidosException | UsuarioNoAutorizadoException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Rechaza la solicitud de registro de un ORGANIZADOR.
     * Cambia su estado de PENDIENTE → RECHAZADO.
     *
     * @param id ID del usuario organizador a rechazar
     */
    // @PreAuthorize("hasRole('ADMIN')")  ← activar con Spring Security
    @PutMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazarOrganizador(@PathVariable Long id) {
        try {
            Usuario rechazado = rechazarOrganizadorUseCase.rechazar(id);
            UsuarioResponseDTO responseDTO = UsuarioDtoMapper.toDto(rechazado);
            return ResponseEntity.ok(responseDTO);

        } catch (UsuarioNoEncontradoException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (DatosInvalidosException | UsuarioNoAutorizadoException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
