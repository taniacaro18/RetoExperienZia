package com.experienzia.impl;

import com.experienzia.dto.EventoDTO;
import com.experienzia.entity.EstadoEvento;
import com.experienzia.entity.EstadoInscripcion;
import com.experienzia.entity.Evento;
import com.experienzia.entity.Inscripcion;
import com.experienzia.entity.TipoEvento;
import com.experienzia.entity.TipoNotificacion;
import com.experienzia.exceptions.CustomException;
import com.experienzia.repository.EventoRepository;
import com.experienzia.repository.InscripcionRepository;
import com.experienzia.service.EventoService;
import com.experienzia.service.NotificacionService;
import com.experienzia.spec.EventoSpecification;
import com.experienzia.spec.EventoSpecification.EventoSearchCriteria;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class EventoServiceImpl implements EventoService {

    /** Aforo máximo permitido por evento (regla de negocio). */
    private static final int AFORO_MAXIMO_PERMITIDO = 600;

    /** Ubicación por defecto cuando el organizador no la especifica. */
    private static final String UBICACION_POR_DEFECTO = "Salón principal";

    private final EventoRepository eventoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final NotificacionService notificacionService;
    private final ModelMapper modelMapper;

    /** Tarifa por hora del evento (configurable). El costo se calcula como precioHora * duracionHoras. */
    @Value("${experienzia.precio-por-hora:100000}")
    private double precioPorHora;

    public EventoServiceImpl(EventoRepository eventoRepository,
                             InscripcionRepository inscripcionRepository,
                             NotificacionService notificacionService,
                             ModelMapper modelMapper) {
        this.eventoRepository = eventoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.notificacionService = notificacionService;
        this.modelMapper = modelMapper;
    }

    @Override
    public EventoDTO crear(EventoDTO dto) {
        if (dto.getOrganizadorId() == null) {
            throw new CustomException("El organizador es requerido.", HttpStatus.BAD_REQUEST);
        }
        validarAforo(dto.getAforoMaximo());
        if (dto.getTipoEvento() == null) {
            throw new CustomException("El tipo de evento es requerido (PUBLICO/PRIVADO).", HttpStatus.BAD_REQUEST);
        }
        validarFechas(dto.getFecha(), dto.getFechaFin());

        Evento evento = modelMapper.map(dto, Evento.class);
        evento.setId(null);
        evento.setAforoMaximo(dto.getAforoMaximo());
        evento.setUbicacion(ubicacionFinal(dto.getUbicacion()));
        evento.setDuracionHoras(calcularDuracionHoras(dto.getFecha(), dto.getFechaFin()));
        evento.setCosto(calcularCosto(evento.getDuracionHoras()));
        evento.setEstado(EstadoEvento.PENDIENTE);
        evento.setAforoActual(0);
        evento.setMotivoRechazo(null);
        evento.setMotivoCancelacion(null);
        return toDto(eventoRepository.save(evento));
    }

    @Override
    public EventoDTO editar(Long id, EventoDTO dto) {
        Evento evento = buscarPorId(id);
        if (dto.getOrganizadorId() == null || !dto.getOrganizadorId().equals(evento.getOrganizadorId())) {
            throw new CustomException("Solo el organizador del evento puede editarlo.", HttpStatus.FORBIDDEN);
        }
        if (evento.getEstado() == EstadoEvento.CANCELADO) {
            throw new CustomException("No se puede editar un evento CANCELADO.", HttpStatus.BAD_REQUEST);
        }
        validarAforo(dto.getAforoMaximo());
        if (dto.getAforoMaximo() < evento.getAforoActual()) {
            throw new CustomException(
                    "El aforo máximo no puede reducirse por debajo de la cantidad actual de asistentes (" + evento.getAforoActual() + ").",
                    HttpStatus.BAD_REQUEST);
        }
        validarFechas(dto.getFecha(), dto.getFechaFin());

        evento.setNombre(dto.getNombre());
        evento.setDescripcion(dto.getDescripcion());
        evento.setFecha(dto.getFecha());
        evento.setFechaFin(dto.getFechaFin());
        evento.setUbicacion(ubicacionFinal(dto.getUbicacion()));
        evento.setAforoMaximo(dto.getAforoMaximo());
        evento.setImagen(dto.getImagen());
        if (dto.getCategoria() != null) evento.setCategoria(dto.getCategoria());
        evento.setDuracionHoras(calcularDuracionHoras(dto.getFecha(), dto.getFechaFin()));
        evento.setCosto(calcularCosto(evento.getDuracionHoras()));
        // HU-008: tras editar, vuelve a PENDIENTE para nueva aprobación del admin.
        evento.setEstado(EstadoEvento.PENDIENTE);
        evento.setMotivoRechazo(null);
        return toDto(eventoRepository.save(evento));
    }

    private void validarAforo(Integer aforo) {
        if (aforo == null || aforo <= 0) {
            throw new CustomException("El aforo máximo debe ser mayor a 0.", HttpStatus.BAD_REQUEST);
        }
        if (aforo > AFORO_MAXIMO_PERMITIDO) {
            throw new CustomException(
                    "El aforo máximo no puede ser mayor a " + AFORO_MAXIMO_PERMITIDO + " personas.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void validarFechas(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null) {
            throw new CustomException("La fecha de inicio es requerida.", HttpStatus.BAD_REQUEST);
        }
        if (fin == null) {
            throw new CustomException("La fecha de finalización es requerida.", HttpStatus.BAD_REQUEST);
        }
        if (!fin.isAfter(inicio)) {
            throw new CustomException(
                    "La fecha de finalización debe ser posterior a la fecha de inicio.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private String ubicacionFinal(String ubicacion) {
        return (ubicacion == null || ubicacion.isBlank()) ? UBICACION_POR_DEFECTO : ubicacion.trim();
    }

    private int calcularDuracionHoras(LocalDateTime inicio, LocalDateTime fin) {
        long minutos = Duration.between(inicio, fin).toMinutes();
        if (minutos <= 0) return 1;
        // Redondeo hacia arriba: 90 minutos = 2 horas (cobra hora completa).
        return (int) Math.ceil(minutos / 60.0);
    }

    private double calcularCosto(int duracionHoras) {
        return precioPorHora * Math.max(1, duracionHoras);
    }

    @Override
    public EventoDTO aprobar(Long id) {
        Evento evento = buscarPorId(id);
        if (evento.getEstado() != EstadoEvento.PENDIENTE) {
            throw new CustomException("Solo se pueden aprobar eventos PENDIENTES.", HttpStatus.BAD_REQUEST);
        }
        evento.setEstado(EstadoEvento.APROBADO);
        return toDto(eventoRepository.save(evento));
    }

    @Override
    public EventoDTO rechazar(Long id, String motivo) {
        Evento evento = buscarPorId(id);
        if (evento.getEstado() != EstadoEvento.PENDIENTE) {
            throw new CustomException("Solo se pueden rechazar eventos PENDIENTES.", HttpStatus.BAD_REQUEST);
        }
        evento.setEstado(EstadoEvento.RECHAZADO);
        evento.setMotivoRechazo(motivo != null && !motivo.isBlank() ? motivo.trim() : null);
        return toDto(eventoRepository.save(evento));
    }

    @Override
    public EventoDTO cancelar(Long id, Long organizadorId, String motivo) {
        Evento evento = buscarPorId(id);
        if (evento.getOrganizadorId() == null || !evento.getOrganizadorId().equals(organizadorId)) {
            throw new CustomException("Solo el organizador puede cancelar el evento.", HttpStatus.FORBIDDEN);
        }
        if (evento.getEstado() == EstadoEvento.CANCELADO) {
            throw new CustomException("El evento ya está cancelado.", HttpStatus.BAD_REQUEST);
        }
        evento.setEstado(EstadoEvento.CANCELADO);
        evento.setMotivoCancelacion(motivo != null && !motivo.isBlank() ? motivo.trim() : null);
        Evento guardado = eventoRepository.save(evento);

        // HU-009: notificar a los inscritos (solo en sistema, sin correo).
        List<Inscripcion> inscripciones = inscripcionRepository.findByEventoId(id);
        for (Inscripcion ins : inscripciones) {
            if (ins.getEstado() == EstadoInscripcion.CANCELADO) continue;
            notificacionService.crear(ins.getUsuarioId(),
                    "El evento \"" + guardado.getNombre() + "\" fue cancelado por el organizador."
                            + (guardado.getMotivoCancelacion() != null
                                    ? " Motivo: " + guardado.getMotivoCancelacion()
                                    : ""),
                    TipoNotificacion.ALERTA);
        }
        return toDto(guardado);
    }

    @Override
    public EventoDTO activarPorPago(Long id) {
        Evento evento = buscarPorId(id);
        if (evento.getEstado() != EstadoEvento.APROBADO) {
            throw new CustomException("El evento debe estar APROBADO antes de activarse.", HttpStatus.BAD_REQUEST);
        }
        evento.setEstado(EstadoEvento.ACTIVO);
        return toDto(eventoRepository.save(evento));
    }

    @Override
    @Transactional(readOnly = true)
    public EventoDTO obtenerPorId(Long id) {
        return toDto(buscarPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoDTO> listarTodos() {
        return eventoRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoDTO> listarCatalogoPublicoActivo() {
        return eventoRepository.findByTipoEventoAndEstado(TipoEvento.PUBLICO, EstadoEvento.ACTIVO)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoDTO> listarPorOrganizador(Long organizadorId) {
        return eventoRepository.findByOrganizadorId(organizadorId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoDTO> buscar(EventoSearchCriteria c) {
        Specification<Evento> spec = Specification.where(EventoSpecification.hasNombre(c.getNombre()))
                .and(EventoSpecification.hasCategoria(c.getCategoria()))
                .and(EventoSpecification.hasTipo(c.getTipoEvento()))
                .and(EventoSpecification.hasEstado(c.getEstado()))
                .and(EventoSpecification.fechaDesde(c.getFechaDesde()))
                .and(EventoSpecification.fechaHasta(c.getFechaHasta()))
                .and(EventoSpecification.hasOrganizador(c.getOrganizadorId()));
        return eventoRepository.findAll(spec).stream().map(this::toDto).toList();
    }

    @Override
    public void aumentarAforo(Long eventoId) {
        Evento evento = buscarPorId(eventoId);
        if (evento.getAforoActual() >= evento.getAforoMaximo()) {
            throw new CustomException("El evento ha alcanzado su aforo máximo.", HttpStatus.CONFLICT);
        }
        evento.setAforoActual(evento.getAforoActual() + 1);
        eventoRepository.save(evento);
    }

    @Override
    public void disminuirAforo(Long eventoId) {
        Evento evento = buscarPorId(eventoId);
        if (evento.getAforoActual() <= 0) {
            throw new CustomException("El aforo actual no puede ser menor a 0.", HttpStatus.CONFLICT);
        }
        evento.setAforoActual(evento.getAforoActual() - 1);
        eventoRepository.save(evento);
    }

    private Evento buscarPorId(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new CustomException("No se encontró el evento con ID: " + id, HttpStatus.NOT_FOUND));
    }

    private EventoDTO toDto(Evento evento) {
        return modelMapper.map(evento, EventoDTO.class);
    }
}
