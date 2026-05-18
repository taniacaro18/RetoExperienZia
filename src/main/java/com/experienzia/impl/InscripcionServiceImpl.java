package com.experienzia.impl;

import com.experienzia.dto.AforoEnVivoDTO;
import com.experienzia.dto.AsistenteEventoDTO;
import com.experienzia.dto.EventoStaffDTO;
import com.experienzia.dto.FilaAsistenteCargaDTO;
import com.experienzia.dto.InscripcionDTO;
import com.experienzia.dto.ResultadoCargaAsistentesDTO;
import com.experienzia.dto.StaffAsignadoDTO;
import com.experienzia.entity.EstadoEvento;
import com.experienzia.entity.EstadoInscripcion;
import com.experienzia.entity.Estado;
import com.experienzia.entity.Evento;
import com.experienzia.entity.FuncionStaff;
import com.experienzia.entity.Inscripcion;
import com.experienzia.entity.Rol;
import com.experienzia.entity.StaffEventoAsignacion;
import com.experienzia.entity.TipoEvento;
import com.experienzia.entity.TipoNotificacion;
import com.experienzia.entity.Usuario;
import com.experienzia.exceptions.CustomException;
import com.experienzia.repository.EventoRepository;
import com.experienzia.repository.InscripcionRepository;
import com.experienzia.repository.StaffEventoAsignacionRepository;
import com.experienzia.repository.UsuarioRepository;
import com.experienzia.service.InscripcionService;
import com.experienzia.service.NotificacionService;
import com.experienzia.util.EventoVentanaUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class InscripcionServiceImpl implements InscripcionService {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    private static final DateTimeFormatter FMT_VENTANA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CO"));

    @Value("${experienzia.eventos.zona-horaria:America/Bogota}")
    private String zonaHorariaEventos;

    private final InscripcionRepository inscripcionRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final StaffEventoAsignacionRepository staffEventoRepository;
    private final NotificacionService notificacionService;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    public InscripcionServiceImpl(InscripcionRepository inscripcionRepository,
                                  EventoRepository eventoRepository,
                                  UsuarioRepository usuarioRepository,
                                  StaffEventoAsignacionRepository staffEventoRepository,
                                  NotificacionService notificacionService,
                                  ModelMapper modelMapper,
                                  PasswordEncoder passwordEncoder) {
        this.inscripcionRepository = inscripcionRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.staffEventoRepository = staffEventoRepository;
        this.notificacionService = notificacionService;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public InscripcionDTO inscribir(Long usuarioId, Long eventoId) {
        return inscribirInterno(usuarioId, eventoId, true);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InscripcionDTO inscribirOrganizadorEnSuEvento(Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new CustomException("El evento no existe.", HttpStatus.NOT_FOUND));
        if (evento.getOrganizadorId() == null) return null;
        Long organizadorId = evento.getOrganizadorId();

        // Si ya tiene una inscripción no cancelada, salimos.
        Optional<Inscripcion> existente = inscripcionRepository
                .findByUsuarioIdAndEventoId(organizadorId, eventoId);
        if (existente.isPresent() && existente.get().getEstado() != EstadoInscripcion.CANCELADO) {
            return toDto(existente.get());
        }

        // Aforo no debe bloquear: el organizador es asistente principal por defecto.
        // Aun así respetamos el cupo: si está lleno, no inscribimos pero tampoco fallamos.
        if (evento.getAforoActual() >= evento.getAforoMaximo()) {
            return null;
        }
        evento.setAforoActual(evento.getAforoActual() + 1);
        eventoRepository.save(evento);

        Inscripcion ins;
        if (existente.isPresent()) {
            ins = existente.get();
            ins.setEstado(EstadoInscripcion.INSCRITO);
            ins.setFechaInscripcion(LocalDateTime.now());
            if (ins.getCodigoQR() == null) ins.setCodigoQR(generarCodigoQR());
        } else {
            ins = new Inscripcion();
            ins.setUsuarioId(organizadorId);
            ins.setEventoId(eventoId);
            ins.setFechaInscripcion(LocalDateTime.now());
            ins.setEstado(EstadoInscripcion.INSCRITO);
            ins.setCodigoQR(generarCodigoQR());
        }
        return toDto(inscripcionRepository.save(ins));
    }

    /**
     * Inscripción centralizada con todas las reglas:
     * - usuario debe estar ACTIVO (D4)
     * - ADMIN no puede inscribirse como asistente (D5)
     * - organizador del evento no se inscribe a su propio evento (D1)
     * - STAFF asignado a este evento no puede inscribirse como asistente del mismo (D2)
     * - autoInscripcion=false (carga del organizador): el evento puede ser PUBLICO o PRIVADO
     * - autoInscripcion=true: el evento debe ser PUBLICO (D3)
     * - el evento debe estar ACTIVO y con cupo disponible
     */
    private InscripcionDTO inscribirInterno(Long usuarioId, Long eventoId, boolean autoInscripcion) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new CustomException("El evento no existe.", HttpStatus.NOT_FOUND));
        if (evento.getEstado() == EstadoEvento.CANCELADO) {
            throw new CustomException("No se puede inscribir a un evento CANCELADO.", HttpStatus.BAD_REQUEST);
        }
        if (evento.getEstado() != EstadoEvento.ACTIVO) {
            throw new CustomException("No se puede inscribir a un evento que no está ACTIVO.", HttpStatus.BAD_REQUEST);
        }
        if (autoInscripcion && evento.getTipoEvento() != TipoEvento.PUBLICO) {
            throw new CustomException("La auto-inscripción solo está habilitada para eventos PÚBLICOS. Los eventos privados son por invitación.",
                    HttpStatus.FORBIDDEN);
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new CustomException("El usuario no existe.", HttpStatus.NOT_FOUND));
        if (usuario.getEstado() != Estado.ACTIVO) {
            throw new CustomException("El usuario no está ACTIVO. Estado actual: " + usuario.getEstado() + ".",
                    HttpStatus.FORBIDDEN);
        }
        if (usuario.getRol() == Rol.ADMIN) {
            throw new CustomException("La cuenta de administrador no puede inscribirse como asistente.",
                    HttpStatus.FORBIDDEN);
        }
        if (evento.getOrganizadorId() != null && evento.getOrganizadorId().equals(usuarioId)) {
            throw new CustomException("El organizador del evento no puede inscribirse como asistente de su propio evento.",
                    HttpStatus.FORBIDDEN);
        }
        if (usuario.getRol() == Rol.STAFF
                && staffEventoRepository.existsByStaffUsuarioIdAndEventoId(usuarioId, eventoId)) {
            throw new CustomException("El STAFF asignado a este evento no puede inscribirse también como asistente.",
                    HttpStatus.FORBIDDEN);
        }

        if (evento.getAforoActual() >= evento.getAforoMaximo()) {
            throw new CustomException("El evento ya no tiene cupo disponible.", HttpStatus.CONFLICT);
        }

        Optional<Inscripcion> existente = inscripcionRepository.findByUsuarioIdAndEventoId(usuarioId, eventoId);
        if (existente.isPresent()) {
            Inscripcion previa = existente.get();
            if (previa.getEstado() != EstadoInscripcion.CANCELADO) {
                throw new CustomException("El usuario ya se encuentra inscrito en este evento.", HttpStatus.CONFLICT);
            }
            evento.setAforoActual(evento.getAforoActual() + 1);
            eventoRepository.save(evento);
            previa.setFechaInscripcion(LocalDateTime.now());
            previa.setEstado(EstadoInscripcion.INSCRITO);
            if (previa.getCodigoQR() == null) {
                previa.setCodigoQR(generarCodigoQR());
            }
            return toDto(inscripcionRepository.save(previa));
        }

        evento.setAforoActual(evento.getAforoActual() + 1);
        eventoRepository.save(evento);

        Inscripcion nueva = new Inscripcion();
        nueva.setUsuarioId(usuarioId);
        nueva.setEventoId(eventoId);
        nueva.setFechaInscripcion(LocalDateTime.now());
        nueva.setEstado(EstadoInscripcion.INSCRITO);
        nueva.setCodigoQR(generarCodigoQR());
        return toDto(inscripcionRepository.save(nueva));
    }

    private String generarCodigoQR() {
        // HU-007/HU-013/HU-015: identificador único universal del ticket.
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public InscripcionDTO cancelar(Long inscripcionId) {
        Inscripcion ins = buscarInscripcion(inscripcionId);
        if (ins.getEstado() == EstadoInscripcion.CANCELADO) {
            throw new CustomException("La inscripción ya está cancelada.", HttpStatus.BAD_REQUEST);
        }
        if (ins.getEstado() == EstadoInscripcion.ASISTIO) {
            throw new CustomException("No se puede cancelar una inscripción si ya se marcó asistencia.", HttpStatus.BAD_REQUEST);
        }
        ins.setEstado(EstadoInscripcion.CANCELADO);

        Evento evento = eventoRepository.findById(ins.getEventoId())
                .orElseThrow(() -> new CustomException("El evento asociado a la inscripción no existe.", HttpStatus.NOT_FOUND));
        if (evento.getAforoActual() > 0) {
            evento.setAforoActual(evento.getAforoActual() - 1);
            eventoRepository.save(evento);
        }
        return toDto(inscripcionRepository.save(ins));
    }

    @Override
    public InscripcionDTO checkIn(Long inscripcionId, Long staffUsuarioId) {
        Inscripcion ins = buscarInscripcion(inscripcionId);
        Evento evento = eventoRepository.findById(ins.getEventoId())
                .orElseThrow(() -> new CustomException("Evento no encontrado.", HttpStatus.NOT_FOUND));
        validarStaffAsignado(staffUsuarioId, evento);
        validarEventoActivoYEnFecha(evento, "registrar asistencia");

        if (ins.getEstado() == EstadoInscripcion.CANCELADO) {
            throw new CustomException("No se puede marcar asistencia en una inscripción cancelada.", HttpStatus.BAD_REQUEST);
        }
        if (ins.getFechaCheckIn() != null) {
            throw new CustomException(
                    "Este QR ya fue utilizado. Primer ingreso registrado a las "
                            + ins.getFechaCheckIn().toLocalTime().withNano(0) + ".",
                    HttpStatus.BAD_REQUEST);
        }
        // Aforo: si llegamos al máximo no se puede dar más check-in (HU-015 cond. 5).
        if (evento.getAforoActual() != 0 && evento.getAforoMaximo() > 0) {
            long presentes = inscripcionRepository.findByEventoId(evento.getId()).stream()
                    .filter(i -> i.getEstado() == EstadoInscripcion.ASISTIO && i.getFechaCheckOut() == null)
                    .count();
            if (presentes >= evento.getAforoMaximo()) {
                throw new CustomException("Aforo máximo alcanzado. No se permiten más ingresos.",
                        HttpStatus.CONFLICT);
            }
        }
        ins.setEstado(EstadoInscripcion.ASISTIO);
        ins.setFechaCheckIn(LocalDateTime.now());
        Inscripcion guardada = inscripcionRepository.save(ins);

        notificacionService.crear(ins.getUsuarioId(),
                "Tu asistencia al evento \"" + evento.getNombre() + "\" fue registrada.",
                TipoNotificacion.INFO);
        InscripcionDTO dto = toDto(guardada);
        enriquecerDatosCheckInEnDto(dto, ins.getUsuarioId(), ins.getEventoId());
        return dto;
    }

    @Override
    public InscripcionDTO checkInPorQR(String codigoQR, Long staffUsuarioId, Long eventoId) {
        if (codigoQR == null || codigoQR.isBlank()) {
            throw new CustomException("Código QR vacío.", HttpStatus.BAD_REQUEST);
        }
        Inscripcion ins = inscripcionRepository.findByCodigoQR(codigoQR.trim())
                .orElseThrow(() -> new CustomException("QR inválido o no registrado.", HttpStatus.NOT_FOUND));
        if (eventoId != null && !eventoId.equals(ins.getEventoId())) {
            throw new CustomException("El QR pertenece a otro evento.", HttpStatus.BAD_REQUEST);
        }
        return checkIn(ins.getId(), staffUsuarioId);
    }

    @Override
    public InscripcionDTO checkOut(Long inscripcionId, Long staffUsuarioId) {
        Inscripcion ins = buscarInscripcion(inscripcionId);
        Evento evento = eventoRepository.findById(ins.getEventoId())
                .orElseThrow(() -> new CustomException("Evento no encontrado.", HttpStatus.NOT_FOUND));
        validarStaffAsignado(staffUsuarioId, evento);
        validarEventoActivoYEnFecha(evento, "registrar salida");

        if (ins.getEstado() != EstadoInscripcion.ASISTIO || ins.getFechaCheckIn() == null) {
            throw new CustomException("Solo se puede marcar salida si la asistencia fue confirmada.", HttpStatus.BAD_REQUEST);
        }
        if (ins.getFechaCheckOut() != null) {
            throw new CustomException(
                    "Este QR ya tiene salida registrada a las "
                            + ins.getFechaCheckOut().toLocalTime().withNano(0) + ".",
                    HttpStatus.BAD_REQUEST);
        }
        ins.setFechaCheckOut(LocalDateTime.now());
        Inscripcion guardada = inscripcionRepository.save(ins);

        notificacionService.crear(ins.getUsuarioId(),
                "Tu salida del evento \"" + evento.getNombre() + "\" fue registrada.",
                TipoNotificacion.INFO);
        InscripcionDTO dto = toDto(guardada);
        enriquecerDatosCheckInEnDto(dto, ins.getUsuarioId(), ins.getEventoId());
        return dto;
    }

    @Override
    public InscripcionDTO checkOutPorQR(String codigoQR, Long staffUsuarioId, Long eventoId) {
        if (codigoQR == null || codigoQR.isBlank()) {
            throw new CustomException("Código QR vacío.", HttpStatus.BAD_REQUEST);
        }
        Inscripcion ins = inscripcionRepository.findByCodigoQR(codigoQR.trim())
                .orElseThrow(() -> new CustomException("QR inválido o no registrado.", HttpStatus.NOT_FOUND));
        if (eventoId != null && !eventoId.equals(ins.getEventoId())) {
            throw new CustomException("El QR pertenece a otro evento.", HttpStatus.BAD_REQUEST);
        }
        return checkOut(ins.getId(), staffUsuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InscripcionDTO> listarPorEvento(Long eventoId) {
        return inscripcionRepository.findByEventoId(eventoId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InscripcionDTO> listarPorUsuario(Long usuarioId) {
        return inscripcionRepository.findByUsuarioId(usuarioId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsistenteEventoDTO> listarAsistentesParaStaff(Long eventoId, Long staffUsuarioId, String busqueda) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new CustomException("No se encontró el evento con ID: " + eventoId, HttpStatus.NOT_FOUND));
        validarStaffAsignado(staffUsuarioId, evento);
        return obtenerAsistentes(evento, busqueda);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsistenteEventoDTO> listarAsistentesParaOrganizador(Long eventoId, Long organizadorId, String busqueda) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new CustomException("No se encontró el evento con ID: " + eventoId, HttpStatus.NOT_FOUND));
        if (organizadorId == null || !organizadorId.equals(evento.getOrganizadorId())) {
            throw new CustomException("Solo el organizador del evento puede consultar sus asistentes.", HttpStatus.FORBIDDEN);
        }
        return obtenerAsistentes(evento, busqueda);
    }

    private List<AsistenteEventoDTO> obtenerAsistentes(Evento evento, String busqueda) {
        String filtro = busqueda == null ? null : busqueda.trim().toLowerCase(Locale.ROOT);
        List<Inscripcion> inscripciones = inscripcionRepository.findByEventoId(evento.getId());
        List<AsistenteEventoDTO> resultado = new ArrayList<>();
        for (Inscripcion ins : inscripciones) {
            if (ins.getEstado() == EstadoInscripcion.CANCELADO) continue;
            Usuario asistente = usuarioRepository.findById(ins.getUsuarioId()).orElse(null);
            if (asistente == null) continue;
            if (filtro != null && !filtro.isBlank() && !coincideFiltro(asistente, ins, filtro)) continue;
            resultado.add(toAsistenteDto(ins, asistente));
        }
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public AforoEnVivoDTO consultarAforoEnVivo(Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new CustomException("No se encontró el evento con ID: " + eventoId, HttpStatus.NOT_FOUND));
        long inscritosActivos = inscripcionRepository.countByEventoIdAndEstado(eventoId, EstadoInscripcion.INSCRITO);
        long asistencias = inscripcionRepository.countByEventoIdAndEstado(eventoId, EstadoInscripcion.ASISTIO);
        long presentes = inscripcionRepository.countByEventoIdAndEstadoAndFechaCheckOutIsNull(eventoId, EstadoInscripcion.ASISTIO);
        long inscritosTotales = inscritosActivos + asistencias;
        long disponibles = Math.max(0L, evento.getAforoMaximo() - inscritosTotales);
        double porcentaje = evento.getAforoMaximo() == 0
                ? 0.0
                : ((double) presentes / evento.getAforoMaximo()) * 100.0;

        AforoEnVivoDTO dto = new AforoEnVivoDTO();
        dto.setEventoId(evento.getId());
        dto.setNombreEvento(evento.getNombre());
        dto.setAforoMaximo(evento.getAforoMaximo());
        dto.setInscritos(inscritosTotales);
        dto.setAsistencias(asistencias);
        dto.setPresentes(presentes);
        dto.setCuposDisponibles(disponibles);
        dto.setPorcentajeOcupacion(Math.round(porcentaje * 100.0) / 100.0);
        return dto;
    }

    @Override
    public ResultadoCargaAsistentesDTO cargarAsistentesManual(Long eventoId, Long organizadorId, List<FilaAsistenteCargaDTO> filas) {
        if (filas == null || filas.isEmpty()) {
            throw new CustomException("Debe enviar al menos una fila de asistentes.", HttpStatus.BAD_REQUEST);
        }
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new CustomException("El evento no existe.", HttpStatus.NOT_FOUND));
        if (!organizadorId.equals(evento.getOrganizadorId())) {
            throw new CustomException("Solo el organizador del evento puede cargar asistentes.", HttpStatus.FORBIDDEN);
        }
        if (evento.getEstado() != EstadoEvento.ACTIVO) {
            throw new CustomException("Solo se pueden cargar asistentes cuando el evento está ACTIVO.", HttpStatus.BAD_REQUEST);
        }

        ResultadoCargaAsistentesDTO res = new ResultadoCargaAsistentesDTO();
        Set<String> emailsLote = new HashSet<>();
        int linea = 0;
        for (FilaAsistenteCargaDTO fila : filas) {
            linea++;
            try {
                procesarFila(evento, fila, res, emailsLote);
            } catch (Exception e) {
                res.getErrores().add("Línea " + linea + ": " + e.getMessage());
            }
        }
        notificacionService.crear(organizadorId,
                String.format("Carga de asistentes finalizada para \"%s\". Cuentas nuevas: %d. Inscripciones registradas: %d. Omitidos: %d. Errores: %d.",
                        evento.getNombre(),
                        res.getCuentasNuevasCreadas(),
                        res.getInscripcionesRegistradas(),
                        res.getFilasOmitidasDuplicadoUOtros(),
                        res.getErrores().size()),
                TipoNotificacion.INFO);
        return res;
    }

    @Override
    public ResultadoCargaAsistentesDTO cargarAsistentesCsv(Long eventoId, Long organizadorId, String contenidoCsv) {
        return cargarAsistentesManual(eventoId, organizadorId, parseCsv(contenidoCsv));
    }

    @Override
    public void asignarStaff(Long eventoId, Long organizadorId, Long staffUsuarioId, FuncionStaff funcion) {
        Evento evento = validarOrganizadorDeEvento(eventoId, organizadorId);
        validarStaffPropio(organizadorId, staffUsuarioId);

        FuncionStaff funcionFinal = funcion == null ? FuncionStaff.GENERAL : funcion;
        StaffEventoAsignacion existente = staffEventoRepository
                .findByStaffUsuarioIdAndEventoId(staffUsuarioId, eventoId)
                .orElse(null);
        if (existente != null) {
            // Si ya estaba asignado, solo actualizamos su función.
            if (existente.getFuncion() != funcionFinal) {
                existente.setFuncion(funcionFinal);
                staffEventoRepository.save(existente);
                notificacionService.crear(staffUsuarioId,
                        "Tu función en el evento \"" + evento.getNombre() + "\" cambió a: " + funcionFinal + ".",
                        TipoNotificacion.INFO);
            }
            return;
        }
        staffEventoRepository.save(new StaffEventoAsignacion(staffUsuarioId, eventoId, funcionFinal));
        notificacionService.crear(staffUsuarioId,
                "Se te asignó al evento \"" + evento.getNombre() + "\" con la función: " + funcionFinal + ".",
                TipoNotificacion.INFO);
    }

    @Override
    public StaffAsignadoDTO cambiarFuncionStaff(Long eventoId, Long organizadorId, Long staffUsuarioId, FuncionStaff funcion) {
        Evento evento = validarOrganizadorDeEvento(eventoId, organizadorId);
        StaffEventoAsignacion asignacion = staffEventoRepository
                .findByStaffUsuarioIdAndEventoId(staffUsuarioId, eventoId)
                .orElseThrow(() -> new CustomException("El staff no está asignado a este evento.", HttpStatus.NOT_FOUND));
        FuncionStaff nueva = funcion == null ? FuncionStaff.GENERAL : funcion;
        asignacion.setFuncion(nueva);
        StaffEventoAsignacion guardada = staffEventoRepository.save(asignacion);
        notificacionService.crear(staffUsuarioId,
                "Tu función en el evento \"" + evento.getNombre() + "\" cambió a: " + nueva + ".",
                TipoNotificacion.INFO);
        Usuario staff = usuarioRepository.findById(staffUsuarioId).orElse(null);
        return toStaffAsignadoDto(guardada, staff);
    }

    @Override
    public void desasignarStaff(Long eventoId, Long organizadorId, Long staffUsuarioId) {
        Evento evento = validarOrganizadorDeEvento(eventoId, organizadorId);
        StaffEventoAsignacion asignacion = staffEventoRepository
                .findByStaffUsuarioIdAndEventoId(staffUsuarioId, eventoId)
                .orElseThrow(() -> new CustomException("El staff no está asignado a este evento.", HttpStatus.NOT_FOUND));
        staffEventoRepository.delete(asignacion);
        notificacionService.crear(staffUsuarioId,
                "Fuiste removido del evento \"" + evento.getNombre() + "\".",
                TipoNotificacion.ALERTA);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> listarStaffIdsPorEvento(Long eventoId) {
        return staffEventoRepository.findByEventoId(eventoId).stream()
                .map(StaffEventoAsignacion::getStaffUsuarioId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffAsignadoDTO> listarStaffPorEvento(Long eventoId) {
        return staffEventoRepository.findByEventoId(eventoId).stream()
                .map(a -> {
                    Usuario u = usuarioRepository.findById(a.getStaffUsuarioId()).orElse(null);
                    return toStaffAsignadoDto(a, u);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoStaffDTO> listarEventosDelStaff(Long staffUsuarioId) {
        return staffEventoRepository.findByStaffUsuarioId(staffUsuarioId).stream()
                .map(a -> {
                    Evento ev = eventoRepository.findById(a.getEventoId()).orElse(null);
                    return toEventoStaffDto(a, ev);
                })
                .filter(d -> d != null)
                .toList();
    }

    private EventoStaffDTO toEventoStaffDto(StaffEventoAsignacion a, Evento ev) {
        if (ev == null) return null;
        EventoStaffDTO dto = new EventoStaffDTO();
        dto.setAsignacionId(a.getId());
        dto.setEventoId(ev.getId());
        dto.setNombreEvento(ev.getNombre());
        dto.setDescripcion(ev.getDescripcion());
        dto.setFechaEvento(ev.getFecha());
        dto.setUbicacion(ev.getUbicacion());
        dto.setTipoEvento(ev.getTipoEvento() != null ? ev.getTipoEvento().name() : null);
        dto.setEstadoEvento(ev.getEstado() != null ? ev.getEstado().name() : null);
        dto.setCategoria(ev.getCategoria());
        dto.setAforoMaximo(ev.getAforoMaximo());
        dto.setAforoActual(ev.getAforoActual());
        dto.setOrganizadorId(ev.getOrganizadorId());
        dto.setFuncion(a.getFuncion() == null ? FuncionStaff.GENERAL.name() : a.getFuncion().name());
        return dto;
    }

    private Evento validarOrganizadorDeEvento(Long eventoId, Long organizadorId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new CustomException("No se encontró el evento con ID: " + eventoId, HttpStatus.NOT_FOUND));
        if (organizadorId == null || !organizadorId.equals(evento.getOrganizadorId())) {
            throw new CustomException("Solo el organizador del evento puede gestionar su staff.", HttpStatus.FORBIDDEN);
        }
        return evento;
    }

    private void validarStaffPropio(Long organizadorId, Long staffUsuarioId) {
        Usuario staff = usuarioRepository.findById(staffUsuarioId)
                .orElseThrow(() -> new CustomException("Usuario staff no encontrado: " + staffUsuarioId, HttpStatus.NOT_FOUND));
        if (staff.getRol() != Rol.STAFF) {
            throw new CustomException("El usuario debe tener rol STAFF.", HttpStatus.FORBIDDEN);
        }
        if (!organizadorId.equals(staff.getOrganizadorId())) {
            throw new CustomException("El STAFF debe haber sido creado por el mismo organizador.", HttpStatus.FORBIDDEN);
        }
        if (staff.getEstado() != Estado.ACTIVO) {
            throw new CustomException("El STAFF no está ACTIVO. Estado actual: " + staff.getEstado() + ".", HttpStatus.FORBIDDEN);
        }
    }

    private StaffAsignadoDTO toStaffAsignadoDto(StaffEventoAsignacion a, Usuario u) {
        StaffAsignadoDTO dto = new StaffAsignadoDTO();
        dto.setAsignacionId(a.getId());
        dto.setStaffUsuarioId(a.getStaffUsuarioId());
        dto.setFuncion(a.getFuncion() == null ? FuncionStaff.GENERAL.name() : a.getFuncion().name());
        if (u != null) {
            dto.setNombre(u.getNombre());
            dto.setEmail(u.getEmail());
            dto.setTelefono(u.getTelefono());
            dto.setEstadoUsuario(u.getEstado() != null ? u.getEstado().name() : null);
        }
        return dto;
    }

    private void procesarFila(Evento evento, FilaAsistenteCargaDTO fila,
                              ResultadoCargaAsistentesDTO res, Set<String> emailsLote) {
        validarCamposMinimos(fila);
        String emailNorm = fila.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!emailNorm.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Correo con formato inválido.");
        }
        if (!emailsLote.add(emailNorm)) {
            res.setFilasOmitidasDuplicadoUOtros(res.getFilasOmitidasDuplicadoUOtros() + 1);
            return;
        }
        String numeroDoc = fila.getNumeroDocumento().trim();
        Optional<Usuario> existente = usuarioRepository.findByEmail(emailNorm);

        if (existente.isPresent()) {
            Usuario u = existente.get();
            // Cualquier rol activo (excepto ADMIN) puede ser invitado. ADMIN se bloquea ya en inscribirInterno.
            if (u.getEstado() != Estado.ACTIVO) {
                throw new IllegalStateException("El correo pertenece a una cuenta no ACTIVA (estado: " + u.getEstado() + ").");
            }
            Optional<Inscripcion> insOpt = inscripcionRepository.findByUsuarioIdAndEventoId(u.getId(), evento.getId());
            if (insOpt.isPresent() && insOpt.get().getEstado() != EstadoInscripcion.CANCELADO) {
                res.setFilasOmitidasDuplicadoUOtros(res.getFilasOmitidasDuplicadoUOtros() + 1);
                return;
            }
            inscribirEnCarga(res, u.getId(), evento, false);
            return;
        }

        Usuario nuevo = new Usuario();
        nuevo.setNombre(fila.getNombre().trim());
        nuevo.setEmail(emailNorm);
        nuevo.setTelefono(blankToNull(fila.getTelefono()));
        nuevo.setTipoDocumento(fila.getTipoDocumento().trim());
        nuevo.setNumeroDocumento(numeroDoc);
        nuevo.setPassword(passwordEncoder.encode(numeroDoc));
        nuevo.setRol(Rol.ASISTENTE);
        nuevo.setEstado(Estado.ACTIVO);
        Usuario guardado = usuarioRepository.save(nuevo);
        res.setCuentasNuevasCreadas(res.getCuentasNuevasCreadas() + 1);

        notificacionService.crear(guardado.getId(),
                String.format("Tu cuenta fue creada para el evento \"%s\". Contraseña inicial: tu número de documento registrado.", evento.getNombre()),
                TipoNotificacion.INFO);
        inscribirEnCarga(res, guardado.getId(), evento, true);
    }

    private void inscribirEnCarga(ResultadoCargaAsistentesDTO res, Long usuarioId, Evento evento, boolean yaNotificadoAlta) {
        try {
            // Carga = invitación del organizador, no auto-inscripción → admite eventos PRIVADOS también.
            inscribirInterno(usuarioId, evento.getId(), false);
            res.setInscripcionesRegistradas(res.getInscripcionesRegistradas() + 1);
            if (!yaNotificadoAlta) {
                notificacionService.crear(usuarioId,
                        String.format("Quedaste inscrito en el evento \"%s\". Usa tu correo para iniciar sesión.", evento.getNombre()),
                        TipoNotificacion.INFO);
            }
        } catch (CustomException e) {
            if (e.getStatus() == HttpStatus.CONFLICT) {
                res.setFilasOmitidasDuplicadoUOtros(res.getFilasOmitidasDuplicadoUOtros() + 1);
            } else {
                throw new IllegalStateException(e.getMessage());
            }
        }
    }

    private void validarCamposMinimos(FilaAsistenteCargaDTO fila) {
        if (fila.getNombre() == null || fila.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (fila.getEmail() == null || fila.getEmail().isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio.");
        }
        if (fila.getTipoDocumento() == null || fila.getTipoDocumento().isBlank()) {
            throw new IllegalArgumentException("El tipo de documento es obligatorio.");
        }
        if (fila.getNumeroDocumento() == null || fila.getNumeroDocumento().isBlank()) {
            throw new IllegalArgumentException("El número de documento es obligatorio (se usa como contraseña inicial).");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static List<FilaAsistenteCargaDTO> parseCsv(String contenidoCsv) {
        if (contenidoCsv == null || contenidoCsv.isBlank()) {
            throw new IllegalArgumentException("El archivo CSV está vacío.");
        }
        String texto = contenidoCsv.charAt(0) == '\uFEFF' ? contenidoCsv.substring(1) : contenidoCsv;
        String[] lineas = texto.split("\\r?\\n");
        List<FilaAsistenteCargaDTO> filas = new ArrayList<>();

        Integer idxNombre = null, idxEmail = null, idxTel = null, idxTipoDoc = null, idxNumDoc = null;
        boolean hayCabecera = false;

        for (String rawLinea : lineas) {
            String linea = rawLinea.trim();
            if (linea.isEmpty()) continue;
            String[] partes = linea.split("\\s*,\\s*", -1);
            if (!hayCabecera && esCabecera(partes)) {
                idxNombre = indiceColumna(partes, "nombre");
                idxEmail = indiceColumna(partes, "email", "correo");
                idxTel = indiceColumna(partes, "telefono", "teléfono", "tel");
                idxTipoDoc = indiceColumna(partes, "tipodocumento", "tipo documento", "tipo_documento");
                idxNumDoc = indiceColumna(partes, "numerodocumento", "numero documento", "numero_documento", "documento");
                if (idxNombre == null || idxEmail == null || idxTipoDoc == null || idxNumDoc == null) {
                    throw new IllegalArgumentException("CSV con cabecera: se requieren columnas nombre, email, tipo de documento y número de documento.");
                }
                hayCabecera = true;
                continue;
            }
            FilaAsistenteCargaDTO f = new FilaAsistenteCargaDTO();
            if (hayCabecera) {
                f.setNombre(getCelda(partes, idxNombre));
                f.setEmail(getCelda(partes, idxEmail));
                if (idxTel != null) f.setTelefono(getCelda(partes, idxTel));
                f.setTipoDocumento(getCelda(partes, idxTipoDoc));
                f.setNumeroDocumento(getCelda(partes, idxNumDoc));
            } else {
                if (partes.length < 5) {
                    throw new IllegalArgumentException("Sin cabecera use 5 columnas: nombre,email,telefono,tipoDocumento,numeroDocumento");
                }
                f.setNombre(partes[0]);
                f.setEmail(partes[1]);
                f.setTelefono(partes[2]);
                f.setTipoDocumento(partes[3]);
                f.setNumeroDocumento(partes[4]);
            }
            filas.add(f);
        }
        if (filas.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron filas de datos en el CSV.");
        }
        return filas;
    }

    private static String getCelda(String[] partes, int idx) {
        if (idx < 0 || idx >= partes.length) return "";
        return partes[idx].trim();
    }

    private static boolean esCabecera(String[] partes) {
        String uno = String.join(",", partes).toLowerCase(Locale.ROOT);
        return uno.contains("email") && uno.contains("nombre");
    }

    private static String normCol(String raw) {
        return raw.trim().replace("\"", "").toLowerCase(Locale.ROOT).replaceAll("[\\s_]+", "");
    }

    private static Integer indiceColumna(String[] partes, String... titulos) {
        for (int i = 0; i < partes.length; i++) {
            String c = normCol(partes[i]);
            for (String t : titulos) {
                if (c.equals(normCol(t))) return i;
            }
        }
        return null;
    }

    private void validarStaffAsignado(Long staffUsuarioId, Evento evento) {
        Usuario staff = usuarioRepository.findById(staffUsuarioId)
                .orElseThrow(() -> new CustomException("Usuario no encontrado: " + staffUsuarioId, HttpStatus.NOT_FOUND));
        if (staff.getRol() != Rol.STAFF || staff.getOrganizadorId() == null) {
            throw new CustomException("La operación solo la puede ejecutar usuario STAFF.", HttpStatus.FORBIDDEN);
        }
        if (!staff.getOrganizadorId().equals(evento.getOrganizadorId())) {
            throw new CustomException("El staff no pertenece al organizador del evento.", HttpStatus.FORBIDDEN);
        }
        if (!staffEventoRepository.existsByStaffUsuarioIdAndEventoId(staffUsuarioId, evento.getId())) {
            throw new CustomException("El staff no está asignado a este evento.", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Check-in/out (QR o manual) solo dentro de la ventana horaria del evento
     * (desde inicio hasta fin), en la zona horaria configurada del negocio.
     */
    private void validarEventoActivoYEnFecha(Evento evento, String accion) {
        if (evento.getEstado() != EstadoEvento.ACTIVO) {
            throw new CustomException(
                    "El evento no está activo. Solo se puede " + accion + " en eventos activos.",
                    HttpStatus.BAD_REQUEST);
        }
        if (evento.getFecha() == null) {
            throw new CustomException("El evento no tiene fecha definida.", HttpStatus.BAD_REQUEST);
        }
        ZoneId zone = EventoVentanaUtil.zoneId(zonaHorariaEventos);
        ZonedDateTime ahora = ZonedDateTime.now(zone);
        ZonedDateTime inicio = EventoVentanaUtil.instanteInicioZoned(evento, zone);
        ZonedDateTime fin = EventoVentanaUtil.instanteFinZoned(evento, zone);
        if (ahora.isBefore(inicio)) {
            throw new CustomException(
                    "Solo se puede " + accion + " a partir del inicio del evento ("
                            + inicio.format(FMT_VENTANA)
                            + "). Aún no ha comenzado la ventana horaria.",
                    HttpStatus.BAD_REQUEST);
        }
        if (ahora.isAfter(fin)) {
            throw new CustomException(
                    "No se puede " + accion + ": el evento ya finalizó. La ventana terminó el "
                            + fin.format(FMT_VENTANA)
                            + ".",
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Coincidencia por texto: nombre (puede incluir apellidos), email, documento, teléfono, tipo doc. o código QR.
     * Si el criterio tiene varias palabras, todas deben aparecer en algún campo (búsqueda tipo “nombre apellido”).
     */
    private static boolean coincideFiltro(Usuario u, Inscripcion ins, String filtro) {
        String hay = construirTextoBusquedaAsistente(u, ins);
        if (hay.isEmpty()) {
            return false;
        }
        for (String token : filtro.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            if (!hay.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private static String construirTextoBusquedaAsistente(Usuario u, Inscripcion ins) {
        StringBuilder sb = new StringBuilder();
        appendNorm(sb, u.getNombre());
        appendNorm(sb, u.getEmail());
        appendNorm(sb, u.getTelefono());
        appendNorm(sb, u.getTipoDocumento());
        appendNorm(sb, u.getNumeroDocumento());
        if (ins.getCodigoQR() != null && !ins.getCodigoQR().isBlank()) {
            appendNorm(sb, ins.getCodigoQR());
        }
        return sb.toString();
    }

    private static void appendNorm(StringBuilder sb, String part) {
        if (part == null || part.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(part.toLowerCase(Locale.ROOT).trim());
    }

    private Inscripcion buscarInscripcion(Long id) {
        return inscripcionRepository.findById(id)
                .orElseThrow(() -> new CustomException("Inscripción no encontrada con ID: " + id, HttpStatus.NOT_FOUND));
    }

    private InscripcionDTO toDto(Inscripcion ins) {
        return modelMapper.map(ins, InscripcionDTO.class);
    }

    /** Añade nombre, documento y datos del evento para pantallas de staff (check-in/out). */
    private void enriquecerDatosCheckInEnDto(InscripcionDTO dto, Long usuarioId, Long eventoId) {
        usuarioRepository.findById(usuarioId).ifPresent(u -> {
            dto.setNombreAsistente(u.getNombre());
            dto.setEmailAsistente(u.getEmail());
            dto.setTipoDocumento(u.getTipoDocumento());
            dto.setNumeroDocumento(u.getNumeroDocumento());
        });
        eventoRepository.findById(eventoId).ifPresent(ev -> {
            dto.setNombreEvento(ev.getNombre());
            dto.setFechaEvento(ev.getFecha());
            dto.setFechaFinEvento(ev.getFechaFin());
            dto.setUbicacionEvento(ev.getUbicacion());
        });
    }

    private AsistenteEventoDTO toAsistenteDto(Inscripcion ins, Usuario u) {
        AsistenteEventoDTO dto = new AsistenteEventoDTO();
        dto.setInscripcionId(ins.getId());
        dto.setUsuarioId(u.getId());
        dto.setNombre(u.getNombre());
        dto.setEmail(u.getEmail());
        dto.setTelefono(u.getTelefono());
        dto.setTipoDocumento(u.getTipoDocumento());
        dto.setNumeroDocumento(u.getNumeroDocumento());
        dto.setEstadoInscripcion(ins.getEstado());
        dto.setFechaInscripcion(ins.getFechaInscripcion());
        dto.setFechaCheckIn(ins.getFechaCheckIn());
        dto.setFechaCheckOut(ins.getFechaCheckOut());
        dto.setCodigoQR(ins.getCodigoQR());
        return dto;
    }
}
