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
import com.experienzia.service.InscripcionService;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private final InscripcionService inscripcionService;
    private final ModelMapper modelMapper;

    /** Tarifa por hora del evento (configurable). El costo se calcula como precioHora * duracionHoras. */
    @Value("${experienzia.precio-por-hora:100000}")
    private double precioPorHora;

    /**
     * Zona usada para interpretar las {@link LocalDateTime} guardadas del evento y comparar con “ahora”
     * (catálogo público, marcar FINALIZADO, etc.). Por defecto Colombia.
     */
    @Value("${experienzia.eventos.zona-horaria:America/Bogota}")
    private String zonaHorariaEventos;

    public EventoServiceImpl(EventoRepository eventoRepository,
                             InscripcionRepository inscripcionRepository,
                             NotificacionService notificacionService,
                             InscripcionService inscripcionService,
                             ModelMapper modelMapper) {
        this.eventoRepository = eventoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.notificacionService = notificacionService;
        this.inscripcionService = inscripcionService;
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
        LocalDateTime inicio = dto.getFecha();
        LocalDateTime finAjustado = ajustarFinCruceMedianoche(inicio, dto.getFechaFin());
        validarFechas(inicio, finAjustado);

        Evento evento = modelMapper.map(dto, Evento.class);
        evento.setId(null);
        evento.setAforoMaximo(dto.getAforoMaximo());
        evento.setUbicacion(ubicacionFinal(dto.getUbicacion()));
        evento.setFechaFin(finAjustado);
        evento.setDuracionHoras(calcularDuracionHoras(inicio, finAjustado));
        evento.setCosto(calcularCosto(evento.getDuracionHoras()));
        evento.setEstado(EstadoEvento.PENDIENTE);
        evento.setAforoActual(0);
        evento.setMotivoRechazo(null);
        evento.setMotivoCancelacion(null);
        return toDto(eventoRepository.save(evento));
    }

    @Override
    public EventoDTO editar(Long id, EventoDTO dto) {
        marcarEventosActivosFinalizados();
        Evento evento = buscarPorId(id);
        if (evento.getEstado() == EstadoEvento.ACTIVO && eventoHaFinalizadoSuVentana(evento)) {
            evento.setEstado(EstadoEvento.FINALIZADO);
            eventoRepository.save(evento);
        }
        if (dto.getOrganizadorId() == null || !dto.getOrganizadorId().equals(evento.getOrganizadorId())) {
            throw new CustomException("Solo el organizador del evento puede editarlo.", HttpStatus.FORBIDDEN);
        }
        if (evento.getEstado() == EstadoEvento.CANCELADO) {
            throw new CustomException("No se puede editar un evento CANCELADO.", HttpStatus.BAD_REQUEST);
        }
        if (evento.getEstado() == EstadoEvento.FINALIZADO) {
            throw new CustomException("No se puede editar un evento FINALIZADO.", HttpStatus.BAD_REQUEST);
        }
        validarAforo(dto.getAforoMaximo());
        if (dto.getAforoMaximo() < evento.getAforoActual()) {
            throw new CustomException(
                    "El aforo máximo no puede reducirse por debajo de la cantidad actual de asistentes (" + evento.getAforoActual() + ").",
                    HttpStatus.BAD_REQUEST);
        }
        LocalDateTime inicio = dto.getFecha();
        LocalDateTime finAjustado = ajustarFinCruceMedianoche(inicio, dto.getFechaFin());
        validarFechas(inicio, finAjustado);

        evento.setNombre(dto.getNombre());
        evento.setDescripcion(dto.getDescripcion());
        evento.setFecha(inicio);
        evento.setFechaFin(finAjustado);
        evento.setUbicacion(ubicacionFinal(dto.getUbicacion()));
        evento.setAforoMaximo(dto.getAforoMaximo());
        evento.setImagen(dto.getImagen());
        if (dto.getCategoria() != null) evento.setCategoria(dto.getCategoria());
        int nuevaDuracion = calcularDuracionHoras(inicio, finAjustado);
        double costoAnterior = evento.getCosto();
        double costoCalculado = calcularCosto(nuevaDuracion);
        // Eventos sin tarifa (costo 0, p. ej. legado sin horas/cálculo) no deben generar cobro al editar.
        double costoFinal = (costoAnterior <= 0) ? 0 : costoCalculado;
        evento.setDuracionHoras(nuevaDuracion);
        evento.setCosto(costoFinal);

        EstadoEvento estadoAnterior = evento.getEstado();
        evento.setMotivoRechazo(null);

        if (costoFinal <= 0) {
            if (estadoAnterior == EstadoEvento.ACTIVO) {
                evento.setEstado(EstadoEvento.ACTIVO);
            } else if (estadoAnterior == EstadoEvento.APROBADO) {
                evento.setEstado(EstadoEvento.APROBADO);
            } else {
                // PENDIENTE o RECHAZADO: sigue el flujo de aprobación admin sin paso de pago.
                evento.setEstado(EstadoEvento.PENDIENTE);
            }
            Evento guardado = eventoRepository.save(evento);
            if (guardado.getEstado() == EstadoEvento.APROBADO) {
                return activarYAcompanarOrganizador(guardado.getId());
            }
            try {
                inscripcionService.inscribirOrganizadorEnSuEvento(guardado.getId());
            } catch (RuntimeException ignored) {
                // No bloquear la edición si la inscripción del organizador falla por cupo u otra causa.
            }
            return toDto(eventoRepository.findById(guardado.getId()).orElse(guardado));
        }

        // HU-008: con tarifa > 0, tras editar vuelve a PENDIENTE para nueva aprobación del admin.
        evento.setEstado(EstadoEvento.PENDIENTE);
        return toDto(eventoRepository.save(evento));
    }

    /** Activa un evento APROBADO e inscribe al organizador (mismo efecto que al aprobar un pago). */
    private EventoDTO activarYAcompanarOrganizador(Long eventoId) {
        activarPorPago(eventoId);
        try {
            inscripcionService.inscribirOrganizadorEnSuEvento(eventoId);
        } catch (RuntimeException ignored) {
        }
        return obtenerPorId(eventoId);
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

    /**
     * Si la hora de fin no es posterior al inicio (p. ej. inicio 22:00 y fin 04:00 el mismo día),
     * interpreta el fin como el día siguiente. Así los clientes que solo envían hora de reloj quedan coherentes.
     */
    private LocalDateTime ajustarFinCruceMedianoche(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null) {
            return fin;
        }
        if (fin.isAfter(inicio)) {
            return fin;
        }
        return fin.plusDays(1);
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
        if (duracionHoras <= 0) {
            return 0;
        }
        return precioPorHora * duracionHoras;
    }

    @Override
    public EventoDTO aprobar(Long id) {
        Evento evento = buscarPorId(id);
        if (evento.getEstado() != EstadoEvento.PENDIENTE) {
            throw new CustomException("Solo se pueden aprobar eventos PENDIENTES.", HttpStatus.BAD_REQUEST);
        }
        evento.setEstado(EstadoEvento.APROBADO);
        Evento guardado = eventoRepository.save(evento);
        if (guardado.getCosto() <= 0) {
            return activarYAcompanarOrganizador(guardado.getId());
        }
        return toDto(guardado);
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
        if (evento.getEstado() == EstadoEvento.FINALIZADO) {
            throw new CustomException("No se puede cancelar un evento ya FINALIZADO.", HttpStatus.BAD_REQUEST);
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
    public EventoDTO obtenerPorId(Long id) {
        marcarEventosActivosFinalizados();
        Evento evento = buscarPorId(id);
        if (evento.getEstado() == EstadoEvento.ACTIVO && eventoHaFinalizadoSuVentana(evento)) {
            evento.setEstado(EstadoEvento.FINALIZADO);
            eventoRepository.save(evento);
        }
        return toDto(evento);
    }

    @Override
    public List<EventoDTO> listarTodos() {
        marcarEventosActivosFinalizados();
        return eventoRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public List<EventoDTO> listarCatalogoPublicoActivo() {
        marcarEventosActivosFinalizados();
        return eventoRepository.findByTipoEventoAndEstado(TipoEvento.PUBLICO, EstadoEvento.ACTIVO)
                .stream()
                .filter((e) -> !eventoHaFinalizadoSuVentana(e))
                .map(this::toCatalogoPublicoDto)
                .toList();
    }

    @Override
    public EventoDTO obtenerParaCatalogoPublico(Long id) {
        marcarEventosActivosFinalizados();
        Evento e = buscarPorId(id);
        if (e.getTipoEvento() != TipoEvento.PUBLICO || e.getEstado() != EstadoEvento.ACTIVO) {
            throw new CustomException("Evento no disponible en el catálogo público.", HttpStatus.NOT_FOUND);
        }
        if (eventoHaFinalizadoSuVentana(e)) {
            e.setEstado(EstadoEvento.FINALIZADO);
            eventoRepository.save(e);
            throw new CustomException("Evento no disponible en el catálogo público.", HttpStatus.NOT_FOUND);
        }
        return toCatalogoPublicoDto(e);
    }

    @Override
    public List<EventoDTO> listarPorOrganizador(Long organizadorId) {
        marcarEventosActivosFinalizados();
        return eventoRepository.findByOrganizadorId(organizadorId).stream().map(this::toDto).toList();
    }

    @Override
    public List<EventoDTO> buscar(EventoSearchCriteria c) {
        marcarEventosActivosFinalizados();
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

    @Override
    public void marcarEventosActivosFinalizados() {
        for (Evento e : eventoRepository.findByEstado(EstadoEvento.ACTIVO)) {
            if (eventoHaFinalizadoSuVentana(e)) {
                e.setEstado(EstadoEvento.FINALIZADO);
                eventoRepository.save(e);
            }
        }
    }

    /**
     * Momento en que termina la ventana del evento: {@code fechaFin} si es coherente
     * (no nula y no estrictamente anterior al inicio); si no, inicio + duración en horas (mínimo 1 h).
     * Así no quedan eventos ACTIVO para siempre cuando {@code fechaFin} quedó mal guardada.
     */
    private static LocalDateTime instanteFinEvento(Evento e) {
        LocalDateTime inicio = e.getFecha();
        LocalDateTime fin = e.getFechaFin();
        if (fin != null && !fin.isBefore(inicio)) {
            return fin;
        }
        int horas = e.getDuracionHoras() != null && e.getDuracionHoras() > 0 ? e.getDuracionHoras() : 1;
        return inicio.plusHours(horas);
    }

    /** Interpreta fecha/hora del evento en la zona configurada y la compara con “ahora” en esa misma zona. */
    private ZoneId zoneIdParaEventos() {
        String z = zonaHorariaEventos != null ? zonaHorariaEventos.trim() : "America/Bogota";
        try {
            return ZoneId.of(z);
        } catch (Exception ex) {
            return ZoneId.systemDefault();
        }
    }

    private ZonedDateTime instanteFinEnZona(Evento e) {
        return instanteFinEvento(e).atZone(zoneIdParaEventos());
    }

    private ZonedDateTime ahoraEnZonaEventos() {
        return ZonedDateTime.now(zoneIdParaEventos());
    }

    /** true si la ventana del evento ya cerró (fin ≤ ahora en la zona del negocio). */
    private boolean eventoHaFinalizadoSuVentana(Evento e) {
        return !instanteFinEnZona(e).isAfter(ahoraEnZonaEventos());
    }

    private Evento buscarPorId(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new CustomException("No se encontró el evento con ID: " + id, HttpStatus.NOT_FOUND));
    }

    private EventoDTO toCatalogoPublicoDto(Evento evento) {
        EventoDTO dto = toDto(evento);
        dto.setCosto(null);
        dto.setAforoMaximo(null);
        dto.setAforoActual(null);
        dto.setOrganizadorId(null);
        return dto;
    }

    private EventoDTO toDto(Evento evento) {
        return modelMapper.map(evento, EventoDTO.class);
    }
}
