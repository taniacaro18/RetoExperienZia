package com.experienzia.adapters.inbound;

import com.experienzia.adapters.dto.LoginRequestDTO;
import com.experienzia.adapters.dto.UsuarioDtoMapper;
import com.experienzia.adapters.dto.UsuarioRequestDTO;
import com.experienzia.adapters.dto.UsuarioResponseDTO;
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

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.registrarUsuarioUseCase = new RegistrarUsuarioUseCase(usuarioRepository);
        this.loginUseCase = new LoginUseCase(usuarioRepository);
        this.listarUsuariosUseCase = new ListarUsuariosUseCase(usuarioRepository);
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@RequestBody UsuarioRequestDTO requestDTO) {
        try {
            // Mapeamos de DTO a Dominio
            Usuario usuario = UsuarioDtoMapper.toDomain(requestDTO);

            Usuario registrado;
            if ("ORGANIZADOR".equalsIgnoreCase(requestDTO.getTipo())) {
                registrado = registrarUsuarioUseCase.registrarOrganizador(usuario);
            } else {
                registrado = registrarUsuarioUseCase.registrarAsistente(usuario);
            }

            // Devolvemos el DTO de respuesta para no exponer la entidad de dominio ni la contraseña
            UsuarioResponseDTO responseDTO = UsuarioDtoMapper.toDto(registrado);
            return ResponseEntity.ok(responseDTO);
            
        } catch (DatosInvalidosException | UsuarioYaExisteException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

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

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        // Obtenemos del dominio y mapeamos a DTOs de salida
        List<UsuarioResponseDTO> usuarios = listarUsuariosUseCase.listarTodos().stream()
                .map(UsuarioDtoMapper::toDto)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(usuarios);
    }
}
