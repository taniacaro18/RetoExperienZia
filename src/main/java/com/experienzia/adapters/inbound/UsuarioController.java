package com.experienzia.adapters.inbound;

import com.experienzia.adapters.dto.CrearStaffRequestDTO;
import com.experienzia.adapters.dto.LoginRequestDTO;
import com.experienzia.adapters.dto.UsuarioDtoMapper;
import com.experienzia.adapters.dto.UsuarioRequestDTO;
import com.experienzia.adapters.dto.UsuarioResponseDTO;
import com.experienzia.application.service.CrearStaffUseCase;
import com.experienzia.application.service.ListarUsuariosUseCase;
import com.experienzia.application.service.LoginUseCase;
import com.experienzia.application.service.RegistrarUsuarioUseCase;
import com.experienzia.domain.exception.CredencialesInvalidasException;
import com.experienzia.domain.exception.DatosInvalidosException;
import com.experienzia.domain.exception.UsuarioNoAutorizadoException;
import com.experienzia.domain.exception.UsuarioNoEncontradoException;
import com.experienzia.domain.exception.UsuarioYaExisteException;
import com.experienzia.domain.model.Usuario;
import com.experienzia.domain.port.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;
    private final LoginUseCase loginUseCase;
    private final ListarUsuariosUseCase listarUsuariosUseCase;
    private final CrearStaffUseCase crearStaffUseCase;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.registrarUsuarioUseCase = new RegistrarUsuarioUseCase(usuarioRepository);
        this.loginUseCase = new LoginUseCase(usuarioRepository);
        this.listarUsuariosUseCase = new ListarUsuariosUseCase(usuarioRepository);
        this.crearStaffUseCase = new CrearStaffUseCase(usuarioRepository);
    }

    /**
     * POST /api/usuarios/registro
     * Registro público: solo ASISTENTE u ORGANIZADOR.
     * El campo "tipo" acepta: "ASISTENTE" o "ORGANIZADOR".
     * ADMIN y STAFF están bloqueados por el caso de uso.
     */
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@RequestBody UsuarioRequestDTO requestDTO) {
        try {
            // Mapear de DTO a Dominio (sin rol asignado aún)
            Usuario usuario = UsuarioDtoMapper.toDomain(requestDTO);

            Usuario registrado;
            if ("ORGANIZADOR".equalsIgnoreCase(requestDTO.getTipo())) {
                registrado = registrarUsuarioUseCase.registrarOrganizador(usuario);
            } else if ("ASISTENTE".equalsIgnoreCase(requestDTO.getTipo())) {
                registrado = registrarUsuarioUseCase.registrarAsistente(usuario);
            } else {
                return ResponseEntity.badRequest()
                        .body("El campo 'tipo' debe ser ASISTENTE u ORGANIZADOR. " +
                              "No se permite registrar ADMIN ni STAFF desde este endpoint.");
            }

            UsuarioResponseDTO responseDTO = UsuarioDtoMapper.toDto(registrado);
            return ResponseEntity.ok(responseDTO);

        } catch (DatosInvalidosException | UsuarioYaExisteException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (UsuarioNoAutorizadoException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    /**
     * POST /api/usuarios/login
     * Solo usuarios con estado ACTIVO pueden autenticarse.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO requestDTO) {
        try {
            Usuario usuario = loginUseCase.login(requestDTO.getEmail(), requestDTO.getPassword());
            UsuarioResponseDTO responseDTO = UsuarioDtoMapper.toDto(usuario);
            return ResponseEntity.ok(responseDTO);

        } catch (UsuarioNoEncontradoException | CredencialesInvalidasException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (UsuarioNoAutorizadoException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    /**
     * POST /api/usuarios/staff
     * Crea un usuario STAFF. Solo puede ser invocado por un ORGANIZADOR.
     * El organizadorId debe enviarse en el cuerpo de la petición.
     * (En producción vendría del token JWT con Spring Security.)
     */
    // @PreAuthorize("hasRole('ORGANIZADOR')")  ← activar con Spring Security
    @PostMapping("/staff")
    public ResponseEntity<?> crearStaff(@RequestBody CrearStaffRequestDTO requestDTO) {
        try {
            // Construir el objeto de dominio del nuevo STAFF (sin rol aún)
            Usuario nuevoStaff = new Usuario();
            nuevoStaff.setNombre(requestDTO.getNombre());
            nuevoStaff.setEmail(requestDTO.getEmail());
            nuevoStaff.setPassword(requestDTO.getPassword());
            nuevoStaff.setTelefono(requestDTO.getTelefono());
            nuevoStaff.setTipoDocumento(requestDTO.getTipoDocumento());
            nuevoStaff.setNumeroDocumento(requestDTO.getNumeroDocumento());

            Usuario staffCreado = crearStaffUseCase.crearStaff(nuevoStaff, requestDTO.getOrganizadorId());
            UsuarioResponseDTO responseDTO = UsuarioDtoMapper.toDto(staffCreado);
            return ResponseEntity.ok(responseDTO);

        } catch (UsuarioNoEncontradoException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (DatosInvalidosException | UsuarioYaExisteException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (UsuarioNoAutorizadoException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    /**
     * GET /api/usuarios
     * Lista todos los usuarios registrados.
     */
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        List<UsuarioResponseDTO> usuarios = listarUsuariosUseCase.listarTodos().stream()
                .map(UsuarioDtoMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(usuarios);
    }
}
