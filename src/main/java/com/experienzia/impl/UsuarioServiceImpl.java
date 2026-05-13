package com.experienzia.impl;

import com.experienzia.dto.ActualizarPerfilDTO;
import com.experienzia.dto.CrearStaffDTO;
import com.experienzia.dto.LoginDTO;
import com.experienzia.dto.RecuperarPasswordDTO;
import com.experienzia.dto.RecuperarPasswordResponseDTO;
import com.experienzia.dto.UsuarioDTO;
import com.experienzia.entity.Estado;
import com.experienzia.entity.Rol;
import com.experienzia.entity.TipoNotificacion;
import com.experienzia.entity.Usuario;
import com.experienzia.exceptions.CustomException;
import com.experienzia.repository.UsuarioRepository;
import com.experienzia.service.NotificacionService;
import com.experienzia.service.UsuarioService;
import com.experienzia.spec.UsuarioSpecification;
import com.experienzia.spec.UsuarioSpecification.UsuarioSearchCriteria;
import org.modelmapper.ModelMapper;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    /** Registro público: solo letras Unicode y espacios. */
    private static final Pattern REG_NOMBRE_REGISTRO = Pattern.compile("^[\\p{L}\\s]+$");
    /** Teléfono de perfil: dígitos y separadores habituales, longitud razonable. */
    private static final String TELEFONO_PERFIL_REGEX = "^[0-9+\\s().-]+$";
    private static final String ALFABETO_PASS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                              NotificacionService notificacionService,
                              ModelMapper modelMapper,
                              PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UsuarioDTO registrar(UsuarioDTO dto) {
        validarDatosObligatorios(dto.getNombre(), dto.getEmail(), dto.getPassword());
        validarFormatoRegistro(dto);
        validarEmailUnico(dto.getEmail());
        validarNumeroDocumentoUnico(dto.getNumeroDocumento());
        validarTelefonoUnico(dto.getTelefono());

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre().trim());
        usuario.setEmail(dto.getEmail().trim().toLowerCase(Locale.ROOT));
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setTelefono(blankToNull(dto.getTelefono()));
        usuario.setTipoDocumento(dto.getTipoDocumento());
        usuario.setNumeroDocumento(blankToNull(dto.getNumeroDocumento()));

        String tipo = dto.getTipo();
        if ("ORGANIZADOR".equalsIgnoreCase(tipo)) {
            usuario.setRol(Rol.ORGANIZADOR);
            usuario.setEstado(Estado.PENDIENTE);
        } else if ("ASISTENTE".equalsIgnoreCase(tipo) || tipo == null) {
            usuario.setRol(Rol.ASISTENTE);
            usuario.setEstado(Estado.ACTIVO);
        } else {
            throw new CustomException(
                    "El campo 'tipo' debe ser ASISTENTE u ORGANIZADOR. ADMIN y STAFF están bloqueados.");
        }

        return toDto(usuarioRepository.save(usuario));
    }

    @Override
    public UsuarioDTO login(LoginDTO dto) {
        if (dto.getEmail() == null || dto.getPassword() == null) {
            throw new CustomException("Email y contraseña son obligatorios.");
        }
        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new CustomException(
                        "No existe una cuenta registrada con este correo.", HttpStatus.UNAUTHORIZED));

        if (!passwordCoincide(dto.getPassword(), usuario.getPassword())) {
            throw new CustomException("La contraseña ingresada es incorrecta.", HttpStatus.UNAUTHORIZED);
        }
        if (usuario.getEstado() != Estado.ACTIVO) {
            throw new CustomException(
                    "Acceso denegado. El estado de la cuenta es: " + usuario.getEstado(),
                    HttpStatus.FORBIDDEN);
        }
        if (!passwordEsBcrypt(usuario.getPassword())) {
            usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
            usuario = usuarioRepository.save(usuario);
        }
        return toDto(usuario);
    }

    @Override
    public UsuarioDTO crearStaff(CrearStaffDTO dto) {
        if (dto.getOrganizadorId() == null) {
            throw new CustomException("organizadorId es obligatorio.");
        }
        Usuario organizador = usuarioRepository.findById(dto.getOrganizadorId())
                .orElseThrow(() -> new CustomException(
                        "No se encontró el organizador con ID: " + dto.getOrganizadorId(),
                        HttpStatus.NOT_FOUND));
        if (organizador.getRol() != Rol.ORGANIZADOR) {
            throw new CustomException("Solo un ORGANIZADOR puede crear usuarios STAFF.",
                    HttpStatus.FORBIDDEN);
        }
        validarDatosObligatorios(dto.getNombre(), dto.getEmail(), dto.getPassword());
        validarEmailUnico(dto.getEmail());
        validarNumeroDocumentoUnico(dto.getNumeroDocumento());
        validarTelefonoUnico(dto.getTelefono());

        Usuario staff = new Usuario();
        staff.setNombre(dto.getNombre().trim());
        staff.setEmail(dto.getEmail().trim().toLowerCase(Locale.ROOT));
        staff.setPassword(passwordEncoder.encode(dto.getPassword()));
        staff.setTelefono(dto.getTelefono());
        staff.setTipoDocumento(dto.getTipoDocumento());
        staff.setNumeroDocumento(dto.getNumeroDocumento());
        staff.setRol(Rol.STAFF);
        staff.setEstado(Estado.ACTIVO);
        staff.setOrganizadorId(dto.getOrganizadorId());

        return toDto(usuarioRepository.save(staff));
    }

    @Override
    public UsuarioDTO aprobarOrganizador(Long id) {
        Usuario u = buscarOFallar(id);
        if (u.getRol() != Rol.ORGANIZADOR) {
            throw new CustomException("Solo se pueden aprobar usuarios con rol ORGANIZADOR.");
        }
        if (u.getEstado() != Estado.PENDIENTE) {
            throw new CustomException(
                    "El usuario no está en estado PENDIENTE. Estado actual: " + u.getEstado());
        }
        u.setEstado(Estado.ACTIVO);
        return toDto(usuarioRepository.save(u));
    }

    @Override
    public UsuarioDTO rechazarOrganizador(Long id) {
        Usuario u = buscarOFallar(id);
        if (u.getRol() != Rol.ORGANIZADOR) {
            throw new CustomException("Solo se pueden rechazar usuarios con rol ORGANIZADOR.");
        }
        if (u.getEstado() != Estado.PENDIENTE) {
            throw new CustomException(
                    "El usuario no está en estado PENDIENTE. Estado actual: " + u.getEstado());
        }
        u.setEstado(Estado.RECHAZADO);
        return toDto(usuarioRepository.save(u));
    }

    @Override
    public UsuarioDTO desactivar(Long id) {
        Usuario u = buscarOFallar(id);
        if (u.getEstado() == Estado.INACTIVO) {
            throw new CustomException("El usuario ya se encuentra desactivado.");
        }
        u.setEstado(Estado.INACTIVO);
        return toDto(usuarioRepository.save(u));
    }

    @Override
    public UsuarioDTO reactivar(Long id) {
        Usuario u = buscarOFallar(id);
        if (u.getEstado() == Estado.ACTIVO) {
            throw new CustomException("El usuario ya se encuentra ACTIVO.");
        }
        if (u.getEstado() != Estado.INACTIVO) {
            throw new CustomException(
                    "Solo se puede reactivar una cuenta en estado INACTIVO. Estado actual: " + u.getEstado() + ".");
        }
        u.setEstado(Estado.ACTIVO);
        return toDto(usuarioRepository.save(u));
    }

    @Override
    public UsuarioDTO desactivarStaffPorOrganizador(Long organizadorId, Long staffId) {
        Usuario staff = validarStaffDelOrganizador(organizadorId, staffId);
        if (staff.getEstado() == Estado.INACTIVO) {
            throw new CustomException("El staff ya se encuentra desactivado.");
        }
        staff.setEstado(Estado.INACTIVO);
        return toDto(usuarioRepository.save(staff));
    }

    @Override
    public UsuarioDTO reactivarStaffPorOrganizador(Long organizadorId, Long staffId) {
        Usuario staff = validarStaffDelOrganizador(organizadorId, staffId);
        if (staff.getEstado() == Estado.ACTIVO) {
            throw new CustomException("El staff ya se encuentra ACTIVO.");
        }
        if (staff.getEstado() != Estado.INACTIVO) {
            throw new CustomException(
                    "Solo se puede reactivar un staff en estado INACTIVO. Estado actual: " + staff.getEstado() + ".");
        }
        staff.setEstado(Estado.ACTIVO);
        return toDto(usuarioRepository.save(staff));
    }

    private Usuario validarStaffDelOrganizador(Long organizadorId, Long staffId) {
        if (organizadorId == null) {
            throw new CustomException("organizadorId es obligatorio.");
        }
        Usuario organizador = buscarOFallar(organizadorId);
        if (organizador.getRol() != Rol.ORGANIZADOR) {
            throw new CustomException("Solo un ORGANIZADOR puede gestionar el estado de su staff.",
                    HttpStatus.FORBIDDEN);
        }
        Usuario staff = buscarOFallar(staffId);
        if (staff.getRol() != Rol.STAFF) {
            throw new CustomException("El usuario indicado no es STAFF.", HttpStatus.BAD_REQUEST);
        }
        if (staff.getOrganizadorId() == null || !staff.getOrganizadorId().equals(organizadorId)) {
            throw new CustomException("El STAFF no pertenece a este organizador.", HttpStatus.FORBIDDEN);
        }
        return staff;
    }

    @Override
    public UsuarioDTO cambiarRol(Long id, String nuevoRol) {
        if (nuevoRol == null || nuevoRol.isBlank()) {
            throw new CustomException("El rol es obligatorio.");
        }
        Usuario u = buscarOFallar(id);
        Rol rol;
        try {
            rol = Rol.valueOf(nuevoRol.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new CustomException("Rol no válido. Use ADMIN, ORGANIZADOR, ASISTENTE o STAFF.");
        }
        if (rol == Rol.STAFF && u.getOrganizadorId() == null) {
            throw new CustomException(
                    "No se puede convertir un usuario en STAFF sin asociarlo a un organizador.");
        }
        u.setRol(rol);
        return toDto(usuarioRepository.save(u));
    }

    @Override
    public UsuarioDTO obtenerPorId(Long id) {
        return toDto(buscarOFallar(id));
    }

    @Override
    public UsuarioDTO actualizarPerfil(Long id, ActualizarPerfilDTO dto) {
        Usuario u = buscarOFallar(id);
        if (dto.getTelefono() != null) {
            String tel = dto.getTelefono().isBlank() ? null : dto.getTelefono().trim();
            if (tel != null) {
                if (tel.length() < 7 || tel.length() > 20) {
                    throw new CustomException("El teléfono debe tener entre 7 y 20 caracteres.");
                }
                if (!tel.matches(TELEFONO_PERFIL_REGEX)) {
                    throw new CustomException(
                            "El teléfono solo puede incluir dígitos, espacios y los símbolos + ( ) - .");
                }
                if (!tel.equals(u.getTelefono()) && usuarioRepository.existsByTelefono(tel)) {
                    throw new CustomException("El teléfono ya pertenece a otro usuario.");
                }
            }
            u.setTelefono(tel);
        }
        if (dto.getNuevaPassword() != null && !dto.getNuevaPassword().isBlank()) {
            validarActorPuedeCambiarContrasenaDe(id);
            String nuevaPass = dto.getNuevaPassword().trim();
            if (nuevaPass.length() < 4) {
                throw new CustomException("La contraseña debe tener al menos 4 caracteres.");
            }
            // HU-005 C03: la nueva contraseña no puede ser igual a la anterior.
            if (passwordCoincide(nuevaPass, u.getPassword())) {
                throw new CustomException("La nueva contraseña no puede ser igual a la anterior.");
            }
            u.setPassword(passwordEncoder.encode(nuevaPass));
        }
        return toDto(usuarioRepository.save(u));
    }

    @Override
    public RecuperarPasswordResponseDTO recuperarPassword(RecuperarPasswordDTO dto) {
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new CustomException("El correo electrónico es obligatorio.");
        }
        if (dto.getNumeroDocumento() == null || dto.getNumeroDocumento().isBlank()) {
            throw new CustomException("El número de documento es obligatorio.");
        }
        Usuario u = usuarioRepository.findByEmail(dto.getEmail().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new CustomException(
                        "No existe un usuario con ese correo electrónico.", HttpStatus.NOT_FOUND));
        if (u.getEstado() != Estado.ACTIVO) {
            throw new CustomException(
                    "Solo cuentas activas pueden recuperar contraseña. Contacta al administrador.",
                    HttpStatus.FORBIDDEN);
        }
        if (u.getNumeroDocumento() == null
                || !u.getNumeroDocumento().trim().equals(dto.getNumeroDocumento().trim())) {
            throw new CustomException(
                    "Los datos de identidad no coinciden con la cuenta.", HttpStatus.UNAUTHORIZED);
        }
        String temporal = generarPasswordTemporal(10);
        u.setPassword(passwordEncoder.encode(temporal));
        Usuario guardado = usuarioRepository.save(u);

        return new RecuperarPasswordResponseDTO(
                guardado.getId(),
                guardado.getEmail(),
                temporal,
                "Se generó una contraseña temporal. Cámbiala desde tu perfil.");
    }

    @Override
    public RecuperarPasswordResponseDTO reenviarCredenciales(Long usuarioId) {
        Usuario u = buscarOFallar(usuarioId);
        if (u.getEstado() != Estado.ACTIVO) {
            throw new CustomException(
                    "Solo se pueden reenviar credenciales a usuarios ACTIVOS. Estado actual: " + u.getEstado() + ".",
                    HttpStatus.FORBIDDEN);
        }
        // Si tiene número de documento (caso típico de carga masiva), volvemos a usarlo como contraseña.
        // Si no, generamos una temporal.
        String nuevaPass = (u.getNumeroDocumento() != null && !u.getNumeroDocumento().isBlank())
                ? u.getNumeroDocumento().trim()
                : generarPasswordTemporal(10);
        u.setPassword(passwordEncoder.encode(nuevaPass));
        Usuario guardado = usuarioRepository.save(u);

        notificacionService.crear(guardado.getId(),
                "Se reenviaron tus credenciales. Tu nueva contraseña inicial es: " + nuevaPass
                        + ". Cámbiala desde tu perfil.",
                TipoNotificacion.INFO);

        return new RecuperarPasswordResponseDTO(
                guardado.getId(),
                guardado.getEmail(),
                nuevaPass,
                "Credenciales reenviadas. La contraseña temporal fue notificada al usuario.");
    }

    @Override
    public List<UsuarioDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UsuarioDTO> buscarPorCriterios(UsuarioSearchCriteria c) {
        Specification<Usuario> spec = Specification.where(UsuarioSpecification.hasNombre(c.getNombre()))
                .and(UsuarioSpecification.hasEmail(c.getEmail()))
                .and(UsuarioSpecification.hasRol(c.getRol()))
                .and(UsuarioSpecification.hasEstado(c.getEstado()))
                .and(UsuarioSpecification.hasOrganizadorId(c.getOrganizadorId()));
        return usuarioRepository.findAll(spec).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ----------------- helpers privados -----------------

    private Usuario buscarOFallar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        "No se encontró un usuario con el ID: " + id, HttpStatus.NOT_FOUND));
    }

    private void validarDatosObligatorios(String nombre, String email, String password) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new CustomException("El nombre es un campo obligatorio.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new CustomException("La contraseña es un campo obligatorio.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new CustomException("El correo electrónico es un campo obligatorio.");
        }
        if (!email.matches(EMAIL_REGEX)) {
            throw new CustomException("El formato del correo electrónico no es válido.");
        }
    }

    /** HU-001/002: reglas de formato del formulario de registro público. */
    private void validarFormatoRegistro(UsuarioDTO dto) {
        String nombre = dto.getNombre().trim();
        if (nombre.length() < 3) {
            throw new CustomException("El nombre debe tener al menos 3 caracteres.");
        }
        if (!REG_NOMBRE_REGISTRO.matcher(nombre).matches()) {
            throw new CustomException("El nombre solo puede contener letras y espacios.");
        }
        String tel = dto.getTelefono() != null ? dto.getTelefono().trim() : "";
        if (tel.isEmpty()) {
            throw new CustomException("El número de celular es obligatorio.");
        }
        if (!tel.matches("^\\d{10}$")) {
            throw new CustomException("El celular debe tener exactamente 10 dígitos numéricos.");
        }
        String doc = dto.getNumeroDocumento() != null ? dto.getNumeroDocumento().trim() : "";
        if (doc.isEmpty()) {
            throw new CustomException("El número de documento es obligatorio.");
        }
        if (!doc.matches("^\\d{4,10}$")) {
            throw new CustomException("El documento solo puede contener números (entre 4 y 10 dígitos).");
        }
        if (dto.getTipoDocumento() == null || dto.getTipoDocumento().isBlank()) {
            throw new CustomException("El tipo de documento es obligatorio.");
        }
        String pwd = dto.getPassword();
        if (pwd.length() < 9) {
            throw new CustomException("La contraseña debe tener más de 8 caracteres (mínimo 9).");
        }
        boolean mayus = pwd.codePoints().anyMatch(Character::isUpperCase);
        boolean minus = pwd.codePoints().anyMatch(Character::isLowerCase);
        boolean especial = pwd.codePoints()
                .anyMatch(cp -> !Character.isLetter(cp) && !Character.isDigit(cp) && !Character.isWhitespace(cp));
        if (!mayus) {
            throw new CustomException("La contraseña debe incluir al menos una letra mayúscula.");
        }
        if (!minus) {
            throw new CustomException("La contraseña debe incluir al menos una letra minúscula.");
        }
        if (!especial) {
            throw new CustomException("La contraseña debe incluir al menos un carácter especial.");
        }
    }

    private void validarEmailUnico(String email) {
        if (usuarioRepository.existsByEmail(email.trim().toLowerCase(Locale.ROOT))) {
            throw new CustomException("El correo electrónico ya se encuentra registrado.");
        }
    }

    /**
     * La contraseña solo puede cambiarse por el propio usuario (perfil).
     * Evita que un administrador u otro rol use PUT /usuarios/{id} para fijar la contraseña de terceros.
     */
    private void validarActorPuedeCambiarContrasenaDe(Long usuarioObjetivoId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long actorId)) {
            throw new CustomException("No autenticado.", HttpStatus.UNAUTHORIZED);
        }
        if (!actorId.equals(usuarioObjetivoId)) {
            throw new CustomException(
                    "Solo puedes cambiar tu propia contraseña desde tu perfil.",
                    HttpStatus.FORBIDDEN);
        }
    }

    private void validarNumeroDocumentoUnico(String numeroDocumento) {
        if (numeroDocumento == null || numeroDocumento.isBlank()) return;
        if (usuarioRepository.existsByNumeroDocumento(numeroDocumento.trim())) {
            throw new CustomException("El número de documento ya se encuentra registrado.");
        }
    }

    private void validarTelefonoUnico(String telefono) {
        if (telefono == null || telefono.isBlank()) return;
        if (usuarioRepository.existsByTelefono(telefono.trim())) {
            throw new CustomException("El teléfono ya se encuentra registrado.");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String generarPasswordTemporal(int longitud) {
        StringBuilder sb = new StringBuilder(longitud);
        for (int i = 0; i < longitud; i++) {
            sb.append(ALFABETO_PASS.charAt(RANDOM.nextInt(ALFABETO_PASS.length())));
        }
        return sb.toString();
    }

    private boolean passwordEsBcrypt(String stored) {
        return stored != null
                && (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"));
    }

    /**
     * BCrypt o contraseñas legadas en texto plano (se migran a BCrypt al iniciar sesión).
     */
    private boolean passwordCoincide(String raw, String stored) {
        if (stored == null || raw == null) {
            return false;
        }
        if (passwordEsBcrypt(stored)) {
            return passwordEncoder.matches(raw, stored);
        }
        return raw.equals(stored);
    }

    private UsuarioDTO toDto(Usuario u) {
        UsuarioDTO dto = modelMapper.map(u, UsuarioDTO.class);
        if (u.getRol() != null) dto.setRol(u.getRol().name());
        if (u.getEstado() != null) dto.setEstado(u.getEstado().name());
        dto.setPassword(null); // nunca devolver la contraseña
        return dto;
    }
}
