package com.experienzia.impl;

import com.experienzia.dto.DisponibilidadSalonDTO;
import com.experienzia.dto.EventoDTO;
import com.experienzia.dto.EventoNovedadDTO;
import com.experienzia.dto.FranjaOcupacionSalonDTO;
import com.experienzia.entity.EstadoEvento;
import com.experienzia.entity.EstadoInscripcion;
import com.experienzia.entity.EstadoNovedadEvento;
import com.experienzia.entity.EstadoPago;
import com.experienzia.entity.Evento;
import com.experienzia.entity.EventoNovedad;
import com.experienzia.entity.Inscripcion;
import com.experienzia.entity.Pago;
import com.experienzia.entity.TipoEvento;
import com.experienzia.entity.TipoNovedadEvento;
import com.experienzia.entity.TipoNotificacion;
import com.experienzia.entity.Usuario;
import com.experienzia.exceptions.CustomException;
import com.experienzia.repository.EventoNovedadRepository;
import com.experienzia.repository.EventoRepository;
import com.experienzia.repository.InscripcionRepository;
import com.experienzia.repository.PagoRepository;
import com.experienzia.repository.UsuarioRepository;
import com.experienzia.service.EventoService;
import com.experienzia.service.FileStorageService;
import com.experienzia.service.InscripcionService;
import com.experienzia.service.NotificacionService;
import com.experienzia.spec.EventoSpecification;
import com.experienzia.spec.EventoSpecification.EventoSearchCriteria;
import com.experienzia.util.EventoVentanaUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class EventoServiceImpl implements EventoService {

    /** Aforo máximo permitido por evento (regla de negocio). */
    private static final int AFORO_MAXIMO_PERMITIDO = 600;

    /** Ubicación por defecto cuando el organizador no la especifica. */
    private static final String UBICACION_POR_DEFECTO = "Salón principal";

    private static final List<EstadoEvento> ESTADOS_RESERVAN_UBICACION = List.of(
            EstadoEvento.PENDIENTE,
            EstadoEvento.APROBADO,
            EstadoEvento.ACTIVO,
            EstadoEvento.PENDIENTE_REVISION,
            EstadoEvento.PENDIENTE_SUPLEMENTO,
            EstadoEvento.PENDIENTE_CANCELACION);

    private static final DateTimeFormatter FMT_VENTANA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CO"));

    private static final double PENALIZACION_POR_HORA_REDUCIDA = 0.05;

    private final EventoRepository eventoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PagoRepository pagoRepository;
    private final EventoNovedadRepository eventoNovedadRepository;
    private final NotificacionService notificacionService;
    private final InscripcionService inscripcionService;
    private final ModelMapper modelMapper;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;

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
                             PagoRepository pagoRepository,
                             EventoNovedadRepository eventoNovedadRepository,
                             NotificacionService notificacionService,
                             InscripcionService inscripcionService,
                             ModelMapper modelMapper,
                             UsuarioRepository usuarioRepository,
                             ObjectMapper objectMapper,
                             FileStorageService fileStorageService) {
        this.eventoRepository = eventoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.pagoRepository = pagoRepository;
        this.eventoNovedadRepository = eventoNovedadRepository;
        this.notificacionService = notificacionService;
        this.inscripcionService = inscripcionService;
        this.modelMapper = modelMapper;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
        this.fileStorageService = fileStorageService;
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
        assertUbicacionDisponible(inicio, finAjustado, dto.getUbicacion(), null);

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
        evento.setEstadoPrevioRevision(null);
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
        if (estadoBloqueaEdicion(evento.getEstado())) {
            throw new CustomException(
                    "Este evento tiene un trámite pendiente de administración o de pago adicional por horas. "
                            + "Espera la resolución antes de volver a editarlo.",
                    HttpStatus.CONFLICT);
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

        LocalDateTime viejoInicio = evento.getFecha();
        LocalDateTime viejoFin = evento.getFechaFin();
        int viejaDuracion = evento.getDuracionHoras() != null && evento.getDuracionHoras() > 0
                ? evento.getDuracionHoras()
                : calcularDuracionHoras(viejoInicio, viejoFin == null ? viejoInicio.plusHours(1) : viejoFin);
        double viejoCosto = evento.getCosto();
        EstadoEvento estadoAntes = evento.getEstado();

        int nuevaDuracion = calcularDuracionHoras(inicio, finAjustado);
        double costoFinal = calcularCosto(nuevaDuracion);

        boolean cambiaNombre = cambiaTexto(evento.getNombre(), dto.getNombre());
        boolean cambiaDescripcion = cambiaTexto(evento.getDescripcion(), dto.getDescripcion());
        boolean cambiaImagen = cambiaTexto(evento.getImagen(), dto.getImagen());
        String ubicNueva = ubicacionFinal(dto.getUbicacion());
        boolean cambiaUbicacion = cambiaTexto(evento.getUbicacion(), ubicNueva);
        boolean cambiaAforo = !Objects.equals(evento.getAforoMaximo(), dto.getAforoMaximo());
        boolean cambiaTipo = dto.getTipoEvento() != null && dto.getTipoEvento() != evento.getTipoEvento();
        boolean cambiaCategoria = dto.getCategoria() != null
                && !normalizarCategoria(dto.getCategoria()).equalsIgnoreCase(normalizarCategoria(evento.getCategoria()));
        boolean cambiaDuracion = nuevaDuracion != viejaDuracion;
        boolean cambiaAgenda = !inicio.equals(viejoInicio) || !finAjustado.equals(viejoFin);

        if (cambiaAgenda || cambiaUbicacion || cambiaDuracion) {
            assertUbicacionDisponible(inicio, finAjustado, ubicNueva, evento.getId());
        }

        boolean requiereRevisionTipoCat = cambiaTipo || cambiaCategoria;
        boolean soloMetadatosSinTipoCatNiHoras = !requiereRevisionTipoCat && !cambiaDuracion;
        boolean aumentaHoras = nuevaDuracion > viejaDuracion;
        boolean disminuyeHoras = nuevaDuracion < viejaDuracion;

        java.util.Map<String, Object> snapshotAntes = snapshotEventoParaRevert(evento);

        evento.setNombre(dto.getNombre());
        evento.setDescripcion(dto.getDescripcion());
        evento.setFecha(inicio);
        evento.setFechaFin(finAjustado);
        evento.setUbicacion(ubicNueva);
        evento.setAforoMaximo(dto.getAforoMaximo());
        evento.setImagen(dto.getImagen());
        if (dto.getCategoria() != null) {
            evento.setCategoria(dto.getCategoria());
        }
        if (dto.getTipoEvento() != null) {
            evento.setTipoEvento(dto.getTipoEvento());
        }
        evento.setDuracionHoras(nuevaDuracion);
        evento.setCosto(costoFinal);
        evento.setMotivoRechazo(null);

        Optional<Pago> pOpt = pagoRepository.findByEventoId(evento.getId());
        boolean pagoAprobado = pOpt.isPresent() && pOpt.get().getEstado() == EstadoPago.APROBADO;

        if (costoFinal <= 0.0) {
            evento.setResumenSolicitudEdicion(null);
            evento.setEstadoPrevioRevision(null);
            if (estadoAntes == EstadoEvento.ACTIVO) {
                evento.setEstado(EstadoEvento.ACTIVO);
            } else if (estadoAntes == EstadoEvento.APROBADO) {
                evento.setEstado(EstadoEvento.APROBADO);
            } else {
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
            return conAlerta(eventoRepository.findByIdWithOrganizador(guardado.getId()).orElse(guardado), null);
        }

        boolean gestionadoActivoOaprobado =
                estadoAntes == EstadoEvento.ACTIVO || estadoAntes == EstadoEvento.APROBADO;

        if (gestionadoActivoOaprobado && aumentaHoras && pagoAprobado) {
            Pago p = pOpt.get();
            double montoYaCobradoAprobado = p.getMonto();
            if (costoFinal > montoYaCobradoAprobado + 0.01) {
                // Fase 1: revisión admin de los cambios (sin tocar el pago aprobado aún).
                evento.setEstadoPrevioRevision(estadoAntes);
                evento.setEstado(EstadoEvento.PENDIENTE_REVISION);
                evento.setResumenSolicitudEdicion(construirResumenEdicion(
                        viejaDuracion,
                        nuevaDuracion,
                        montoYaCobradoAprobado,
                        costoFinal,
                        cambiaNombre,
                        cambiaDescripcion,
                        cambiaImagen,
                        cambiaUbicacion,
                        cambiaAforo,
                        cambiaTipo,
                        cambiaCategoria,
                        true,
                        cambiaAgenda));
                Evento guardado = eventoRepository.save(evento);
                registrarNovedadAumentoHoras(
                        guardado, dto.getOrganizadorId(), viejaDuracion, nuevaDuracion, p, costoFinal, snapshotAntes);
                double adicional = costoFinal - montoYaCobradoAprobado;
                String msg = "Aumentaste las horas: un administrador debe aprobar los cambios primero. Después deberás pagar solo el excedente ("
                        + copTexto(adicional)
                        + " COP) y subir un comprobante por esa diferencia.";
                return conAlerta(guardado, msg);
            }
            // Horas facturadas aumentaron pero el cobro no supera lo ya aprobado (inconsistencia o tarifa ya al día):
            // nunca dejar ACTIVO sin revisión ni permitir "gratis" silencioso.
            evento.setEstadoPrevioRevision(estadoAntes);
            evento.setEstado(EstadoEvento.PENDIENTE_REVISION);
            evento.setResumenSolicitudEdicion(construirResumenEdicion(
                    viejaDuracion,
                    nuevaDuracion,
                    viejoCosto,
                    costoFinal,
                    cambiaNombre,
                    cambiaDescripcion,
                    cambiaImagen,
                    cambiaUbicacion,
                    cambiaAforo,
                    cambiaTipo,
                    cambiaCategoria,
                    true,
                    cambiaAgenda));
            Evento guardado = eventoRepository.save(evento);
            registrarNovedadEdicionBasica(
                    guardado,
                    dto.getOrganizadorId(),
                    TipoNovedadEvento.AUMENTO_HORAS,
                    snapshotAntes,
                    guardado.getResumenSolicitudEdicion());
            return conAlerta(
                    guardado,
                    "Aumentaste horas respecto al pago ya aprobado, pero el sistema no detecta saldo adicional pendiente. "
                            + "Los cambios quedaron a revisión administrativa para validar tarifa y estado del pago.");
        }

        if (gestionadoActivoOaprobado && disminuyeHoras) {
            int horasRed = viejaDuracion - nuevaDuracion;
            double baseMonto = pagoAprobado ? pOpt.get().getMonto() : viejoCosto;
            double penal = baseMonto * PENALIZACION_POR_HORA_REDUCIDA * horasRed;
            evento.setEstadoPrevioRevision(estadoAntes);
            evento.setEstado(EstadoEvento.PENDIENTE_REVISION);
            evento.setResumenSolicitudEdicion(construirResumenEdicion(
                    viejaDuracion,
                    nuevaDuracion,
                    viejoCosto,
                    costoFinal,
                    cambiaNombre,
                    cambiaDescripcion,
                    cambiaImagen,
                    cambiaUbicacion,
                    cambiaAforo,
                    cambiaTipo,
                    cambiaCategoria,
                    true,
                    cambiaAgenda)
                    + " · Penalización estimada (5% por hora reducida): " + copTexto(penal) + " COP.");
            Evento guardado = eventoRepository.save(evento);
            registrarNovedadDisminucionHoras(
                    guardado, dto.getOrganizadorId(), viejaDuracion, nuevaDuracion, penal, snapshotAntes);
            String msg = "Se aplicará una penalización del 5% por cada hora reducida. No se reembolsa el valor completo del pago. "
                    + "El administrador debe aprobar el cambio.";
            return conAlerta(guardado, msg);
        }

        if (gestionadoActivoOaprobado
                && ((soloMetadatosSinTipoCatNiHoras
                                && (cambiaNombre
                                        || cambiaDescripcion
                                        || cambiaImagen
                                        || cambiaUbicacion
                                        || cambiaAforo
                                        || cambiaAgenda))
                        || requiereRevisionTipoCat)) {
            evento.setEstadoPrevioRevision(estadoAntes);
            evento.setEstado(EstadoEvento.PENDIENTE_REVISION);
            TipoNovedadEvento tipoNov = requiereRevisionTipoCat
                    ? TipoNovedadEvento.EDICION_TIPO_CATEGORIA
                    : TipoNovedadEvento.EDICION_METADATOS;
            evento.setResumenSolicitudEdicion(construirResumenEdicion(
                    viejaDuracion,
                    nuevaDuracion,
                    viejoCosto,
                    costoFinal,
                    cambiaNombre,
                    cambiaDescripcion,
                    cambiaImagen,
                    cambiaUbicacion,
                    cambiaAforo,
                    cambiaTipo,
                    cambiaCategoria,
                    cambiaDuracion,
                    cambiaAgenda));
            Evento guardado = eventoRepository.save(evento);
            registrarNovedadEdicionBasica(
                    guardado, dto.getOrganizadorId(), tipoNov, snapshotAntes, guardado.getResumenSolicitudEdicion());
            String msg = requiereRevisionTipoCat
                    ? "Cambiaste modalidad o categoría: el evento queda pendiente de aprobación administrativa (sin nuevo pago salvo reglas de horas)."
                    : "Los cambios quedaron pendientes de aprobación del administrador. No se solicita nuevo pago mientras no aumente la duración facturada.";
            return conAlerta(guardado, msg);
        }

        if (cambiaDuracion && (estadoAntes == EstadoEvento.APROBADO || estadoAntes == EstadoEvento.ACTIVO)) {
            if (pOpt.isPresent() && pOpt.get().getEstado() == EstadoPago.PENDIENTE
                    && pOpt.get().getSaldoAprobadoPrevio() == null) {
                Pago pp = pOpt.get();
                boolean tuvoCambioTarifa = Math.abs(costoFinal - viejoCosto) > 0.01;
                pp.setMonto(costoFinal);
                String msgTarifa = null;
                if (tuvoCambioTarifa) {
                    limpiarComprobantePago(pp);
                    msgTarifa =
                            "Cambió la tarifa del evento: debes subir un nuevo comprobante en la sección Pagos antes de que el administrador lo valide.";
                }
                pagoRepository.save(pp);
                evento.setEstado(estadoAntes);
                evento.setResumenSolicitudEdicion(null);
                evento.setEstadoPrevioRevision(null);
                return conAlerta(eventoRepository.save(evento), msgTarifa);
            }

            boolean necesitaRevisionEvento = pOpt.isEmpty() || pOpt.get().getEstado() == EstadoPago.RECHAZADO;
            if (necesitaRevisionEvento) {
                evento.setEstado(EstadoEvento.PENDIENTE);
                evento.setEstadoPrevioRevision(null);
                evento.setResumenSolicitudEdicion(construirResumenEdicion(
                        viejaDuracion, nuevaDuracion, viejoCosto, costoFinal, cambiaNombre, cambiaDescripcion,
                        cambiaImagen, cambiaUbicacion, cambiaAforo, cambiaTipo, cambiaCategoria,
                        cambiaDuracion, cambiaAgenda));
                return conAlerta(eventoRepository.save(evento), null);
            }

            // Pago complementario (delta) pendiente de comprobante: el evento debe seguir en flujo de suplemento.
            if (pOpt.get().getEstado() == EstadoPago.PENDIENTE && pOpt.get().getSaldoAprobadoPrevio() != null) {
                evento.setEstado(EstadoEvento.PENDIENTE_SUPLEMENTO);
                if (evento.getEstadoPrevioRevision() == null) {
                    evento.setEstadoPrevioRevision(estadoAntes);
                }
                return conAlerta(
                        eventoRepository.save(evento),
                        "Tienes un pago adicional pendiente de validación. Completa el comprobante o espera al administrador.");
            }

            // Pago ya aprobado + cambio de duración que no encajó arriba: nunca aplicar silenciosamente.
            if (pOpt.get().getEstado() == EstadoPago.APROBADO) {
                evento.setEstadoPrevioRevision(estadoAntes);
                evento.setEstado(EstadoEvento.PENDIENTE_REVISION);
                evento.setResumenSolicitudEdicion(construirResumenEdicion(
                        viejaDuracion,
                        nuevaDuracion,
                        viejoCosto,
                        costoFinal,
                        cambiaNombre,
                        cambiaDescripcion,
                        cambiaImagen,
                        cambiaUbicacion,
                        cambiaAforo,
                        cambiaTipo,
                        cambiaCategoria,
                        cambiaDuracion,
                        cambiaAgenda));
                Evento guardado = eventoRepository.save(evento);
                registrarNovedadEdicionBasica(
                        guardado,
                        dto.getOrganizadorId(),
                        TipoNovedadEvento.EDICION_METADATOS,
                        snapshotAntes,
                        guardado.getResumenSolicitudEdicion());
                return conAlerta(
                        guardado,
                        "Cambio de duración con pago ya aprobado: queda a revisión administrativa para validar tarifa y estado.");
            }

            evento.setEstado(estadoAntes);
            evento.setResumenSolicitudEdicion(null);
            evento.setEstadoPrevioRevision(null);
            return conAlerta(eventoRepository.save(evento), null);
        }

        evento.setEstado(EstadoEvento.PENDIENTE);
        evento.setEstadoPrevioRevision(null);
        evento.setResumenSolicitudEdicion(construirResumenEdicion(
                viejaDuracion, nuevaDuracion, viejoCosto, costoFinal, cambiaNombre, cambiaDescripcion,
                cambiaImagen, cambiaUbicacion, cambiaAforo, cambiaTipo, cambiaCategoria,
                cambiaDuracion, cambiaAgenda));
        return conAlerta(eventoRepository.save(evento), null);
    }

    private static String normalizarCategoria(String cat) {
        return cat == null || cat.isBlank() ? "" : cat.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean cambiaTexto(String actual, String nuevo) {
        String a = actual == null ? "" : actual.trim();
        String b = nuevo == null ? "" : nuevo.trim();
        return !a.equals(b);
    }

    /** Borra el archivo anterior y deja el pago sin comprobante hasta nueva carga. */
    private void limpiarComprobantePago(Pago p) {
        String url = p.getComprobanteUrl();
        if (url != null && !url.isBlank()) {
            fileStorageService.borrarComprobantePublico(url);
        }
        p.setComprobanteUrl(null);
    }

    private void prepararComplementoPago(Pago p, double nuevoCostoEvento) {
        if (p.getEstado() != EstadoPago.APROBADO) {
            return;
        }
        double montoPrevio = p.getMonto();
        if (nuevoCostoEvento <= montoPrevio + 0.01) {
            return;
        }
        double delta = nuevoCostoEvento - montoPrevio;
        p.setSaldoAprobadoPrevio(montoPrevio);
        p.setMonto(delta);
        p.setEstado(EstadoPago.PENDIENTE);
        limpiarComprobantePago(p);
        p.setMotivoRechazo(null);
        p.setAprobadorId(null);
        p.setFechaResolucion(null);
        pagoRepository.save(p);
    }

    private static String construirResumenEdicion(
            int viejaDuracion,
            int nuevaDuracion,
            double viejoCosto,
            double nuevoCosto,
            boolean cambiaNombre,
            boolean cambiaDescripcion,
            boolean cambiaImagen,
            boolean cambiaUbicacion,
            boolean cambiaAforo,
            boolean cambiaTipo,
            boolean cambiaCategoria,
            boolean cambiaDuracion,
            boolean cambiaAgenda) {
        List<String> partes = new ArrayList<>();
        if (cambiaNombre) {
            partes.add("Nombre");
        }
        if (cambiaDescripcion) {
            partes.add("Descripción");
        }
        if (cambiaImagen) {
            partes.add("Imagen");
        }
        if (cambiaUbicacion) {
            partes.add("Ubicación");
        }
        if (cambiaAforo) {
            partes.add("Aforo máximo");
        }
        if (cambiaTipo) {
            partes.add("Modalidad (público/privado)");
        }
        if (cambiaCategoria) {
            partes.add("Categoría");
        }
        if (cambiaDuracion || Math.abs(nuevoCosto - viejoCosto) > 0.01) {
            partes.add(String.format(Locale.ROOT,
                    "Duración/tarifa: %d h (%s COP) → %d h (%s COP)",
                    viejaDuracion, copTexto(viejoCosto), nuevaDuracion, copTexto(nuevoCosto)));
        } else if (cambiaAgenda) {
            partes.add("Fechas u horario del evento (misma duración facturada)");
        }
        if (partes.isEmpty()) {
            return "Edición de solicitud (revisar datos del evento).";
        }
        return String.join(" · ", partes);
    }

    private static boolean estadoBloqueaEdicion(EstadoEvento e) {
        return e == EstadoEvento.PENDIENTE_REVISION
                || e == EstadoEvento.PENDIENTE_SUPLEMENTO
                || e == EstadoEvento.PENDIENTE_CANCELACION;
    }

    private EventoDTO conAlerta(Evento evento, String alerta) {
        EventoDTO dto = toDto(evento);
        dto.setAlertaNegocio(alerta);
        return dto;
    }

    private java.util.Map<String, Object> snapshotEventoParaRevert(Evento e) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("nombre", e.getNombre());
        m.put("descripcion", e.getDescripcion());
        m.put("fecha", e.getFecha() != null ? e.getFecha().toString() : null);
        m.put("fechaFin", e.getFechaFin() != null ? e.getFechaFin().toString() : null);
        m.put("ubicacion", e.getUbicacion());
        m.put("aforoMaximo", e.getAforoMaximo());
        m.put("tipoEvento", e.getTipoEvento() != null ? e.getTipoEvento().name() : null);
        m.put("categoria", e.getCategoria());
        m.put("duracionHoras", e.getDuracionHoras());
        m.put("costo", e.getCosto());
        m.put("imagen", e.getImagen());
        return m;
    }

    private void aplicarSnapshotEvento(Evento evento, JsonNode antes) throws Exception {
        if (antes == null || antes.isNull()) {
            return;
        }
        if (antes.hasNonNull("nombre")) {
            evento.setNombre(antes.get("nombre").asText());
        }
        if (antes.hasNonNull("descripcion")) {
            evento.setDescripcion(antes.get("descripcion").asText());
        }
        if (antes.hasNonNull("fecha")) {
            evento.setFecha(LocalDateTime.parse(antes.get("fecha").asText()));
        }
        if (antes.hasNonNull("fechaFin")) {
            evento.setFechaFin(LocalDateTime.parse(antes.get("fechaFin").asText()));
        }
        if (antes.hasNonNull("ubicacion")) {
            evento.setUbicacion(antes.get("ubicacion").asText());
        }
        if (antes.has("aforoMaximo")) {
            evento.setAforoMaximo(antes.get("aforoMaximo").asInt());
        }
        if (antes.hasNonNull("tipoEvento")) {
            evento.setTipoEvento(TipoEvento.valueOf(antes.get("tipoEvento").asText()));
        }
        if (antes.has("categoria")) {
            evento.setCategoria(antes.get("categoria").isNull() ? null : antes.get("categoria").asText());
        }
        if (antes.has("duracionHoras")) {
            evento.setDuracionHoras(antes.get("duracionHoras").asInt());
        }
        if (antes.has("costo")) {
            evento.setCosto(antes.get("costo").asDouble());
        }
        if (antes.has("imagen")) {
            evento.setImagen(antes.get("imagen").isNull() ? null : antes.get("imagen").asText());
        }
    }

    private void registrarNovedadEdicionBasica(
            Evento evento,
            Long orgId,
            TipoNovedadEvento tipo,
            java.util.Map<String, Object> eventoAntes,
            String resumen) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            if (evento.getEstadoPrevioRevision() != null) {
                root.put("estadoPrevio", evento.getEstadoPrevioRevision().name());
            }
            root.set("eventoAntes", objectMapper.valueToTree(eventoAntes));
            root.put("resumen", resumen != null ? resumen : "");
            EventoNovedad n = new EventoNovedad();
            n.setEventoId(evento.getId());
            n.setUsuarioSolicitanteId(orgId);
            n.setTipo(tipo);
            n.setEstado(EstadoNovedadEvento.PENDIENTE);
            n.setFechaSolicitud(LocalDateTime.now());
            n.setDetalleJson(objectMapper.writeValueAsString(root));
            eventoNovedadRepository.save(n);
        } catch (Exception ex) {
            throw new CustomException("No se pudo registrar la novedad del evento.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void registrarNovedadAumentoHoras(
            Evento evento,
            Long orgId,
            int horasAntes,
            int horasDespues,
            Pago p,
            double nuevoCostoEvento,
            java.util.Map<String, Object> snapshotAntes) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put(
                    "estadoPrevio",
                    evento.getEstadoPrevioRevision() != null ? evento.getEstadoPrevioRevision().name() : "");
            root.set("eventoAntes", objectMapper.valueToTree(snapshotAntes));
            root.put("horasAntes", horasAntes);
            root.put("horasDespues", horasDespues);
            double montoPrevio = p.getSaldoAprobadoPrevio() != null ? p.getSaldoAprobadoPrevio() : p.getMonto();
            root.put("montoPagadoPrevio", montoPrevio);
            root.put("montoAdicional", Math.max(0, nuevoCostoEvento - montoPrevio));
            EventoNovedad n = new EventoNovedad();
            n.setEventoId(evento.getId());
            n.setUsuarioSolicitanteId(orgId);
            n.setTipo(TipoNovedadEvento.AUMENTO_HORAS);
            n.setEstado(EstadoNovedadEvento.PENDIENTE);
            n.setFechaSolicitud(LocalDateTime.now());
            n.setDetalleJson(objectMapper.writeValueAsString(root));
            eventoNovedadRepository.save(n);
        } catch (Exception ex) {
            throw new CustomException("No se pudo registrar la novedad del evento.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void registrarNovedadDisminucionHoras(
            Evento evento,
            Long orgId,
            int horasAntes,
            int horasDespues,
            double penalizacion,
            java.util.Map<String, Object> snapshotAntes) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put(
                    "estadoPrevio",
                    evento.getEstadoPrevioRevision() != null ? evento.getEstadoPrevioRevision().name() : "");
            root.set("eventoAntes", objectMapper.valueToTree(snapshotAntes));
            root.put("horasAntes", horasAntes);
            root.put("horasDespues", horasDespues);
            root.put("horasReducidas", horasAntes - horasDespues);
            root.put("penalizacionEstimada", penalizacion);
            EventoNovedad n = new EventoNovedad();
            n.setEventoId(evento.getId());
            n.setUsuarioSolicitanteId(orgId);
            n.setTipo(TipoNovedadEvento.DISMINUCION_HORAS);
            n.setEstado(EstadoNovedadEvento.PENDIENTE);
            n.setFechaSolicitud(LocalDateTime.now());
            n.setDetalleJson(objectMapper.writeValueAsString(root));
            eventoNovedadRepository.save(n);
        } catch (Exception ex) {
            throw new CustomException("No se pudo registrar la novedad del evento.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void registrarNovedadCancelacionSolicitud(Evento evento, Long orgId, double valorPagado) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put(
                    "estadoPrevio",
                    evento.getEstadoPrevioRevision() != null ? evento.getEstadoPrevioRevision().name() : "");
            root.put("motivo", evento.getMotivoCancelacion());
            root.put("valorPagadoPlataforma", valorPagado);
            root.put("reembolsoPropuesto70", valorPagado * 0.70);
            EventoNovedad n = new EventoNovedad();
            n.setEventoId(evento.getId());
            n.setUsuarioSolicitanteId(orgId);
            n.setTipo(TipoNovedadEvento.CANCELACION_SOLICITUD);
            n.setEstado(EstadoNovedadEvento.PENDIENTE);
            n.setFechaSolicitud(LocalDateTime.now());
            n.setDetalleJson(objectMapper.writeValueAsString(root));
            eventoNovedadRepository.save(n);
        } catch (Exception ex) {
            throw new CustomException("No se pudo registrar la novedad del evento.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean tieneRevisionPendienteConSuplementoHoras(Long eventoId) {
        return eventoNovedadRepository
                .findFirstByEventoIdAndEstadoAndTipoOrderByFechaSolicitudDesc(
                        eventoId, EstadoNovedadEvento.PENDIENTE, TipoNovedadEvento.AUMENTO_HORAS)
                .isPresent();
    }

    private void marcarUltimaNovedadResuelta(Long eventoId, EstadoNovedadEvento resolucion, String motivo) {
        eventoNovedadRepository
                .findFirstByEventoIdAndEstadoOrderByFechaSolicitudDesc(eventoId, EstadoNovedadEvento.PENDIENTE)
                .ifPresent(n -> {
                    n.setEstado(resolucion);
                    n.setFechaResolucion(LocalDateTime.now());
                    n.setMotivoResolucion(motivo);
                    eventoNovedadRepository.save(n);
                });
    }

    private void revertirEventoDesdeUltimaNovedad(Evento evento) throws Exception {
        Optional<EventoNovedad> nov = eventoNovedadRepository
                .findFirstByEventoIdAndEstadoOrderByFechaSolicitudDesc(evento.getId(), EstadoNovedadEvento.PENDIENTE);
        if (nov.isEmpty()) {
            return;
        }
        JsonNode root = objectMapper.readTree(nov.get().getDetalleJson());
        if (root.has("eventoAntes")) {
            aplicarSnapshotEvento(evento, root.get("eventoAntes"));
        }
    }

    private static String copTexto(double v) {
        return String.format(Locale.ROOT, "%.0f", v);
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

    /**
     * No permite dos eventos en la misma ubicación con horarios que se solapan
     * (incluye solicitudes pendientes de aprobación).
     */
    private void assertUbicacionDisponible(
            LocalDateTime inicio, LocalDateTime fin, String ubicacion, Long excluirEventoId) {
        String ub = ubicacionFinal(ubicacion);
        List<Evento> existentes =
                eventoRepository.findByUbicacionNormalizadaYEstadoIn(ub, ESTADOS_RESERVAN_UBICACION);
        for (Evento otro : existentes) {
            if (excluirEventoId != null && excluirEventoId.equals(otro.getId())) {
                continue;
            }
            if (EventoVentanaUtil.ventanasSeSolapan(inicio, fin, otro)) {
                LocalDateTime otroFin = EventoVentanaUtil.instanteFin(otro);
                throw new CustomException(
                        String.format(
                                Locale.ROOT,
                                "La ubicación «%s» no está disponible entre %s y %s. "
                                        + "Ya existe el evento «%s» programado de %s a %s (estado %s). "
                                        + "Elige otra fecha, otro horario u otra ubicación.",
                                ub,
                                inicio.format(FMT_VENTANA),
                                fin.format(FMT_VENTANA),
                                otro.getNombre(),
                                otro.getFecha().format(FMT_VENTANA),
                                otroFin.format(FMT_VENTANA),
                                otro.getEstado()),
                        HttpStatus.CONFLICT);
            }
        }
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
        assertUbicacionDisponible(
                evento.getFecha(),
                EventoVentanaUtil.instanteFin(evento),
                evento.getUbicacion(),
                evento.getId());
        if (evento.getEstado() == EstadoEvento.PENDIENTE_REVISION) {
            if (tieneRevisionPendienteConSuplementoHoras(evento.getId())) {
                Pago pago = pagoRepository
                        .findByEventoId(evento.getId())
                        .orElseThrow(() -> new CustomException(
                                "No hay registro de pago para validar el suplemento por horas adicionales.",
                                HttpStatus.BAD_REQUEST));
                if (pago.getEstado() != EstadoPago.APROBADO) {
                    throw new CustomException(
                            "El suplemento por horas adicionales requiere un pago base ya aprobado.",
                            HttpStatus.BAD_REQUEST);
                }
                prepararComplementoPago(pago, evento.getCosto());
                evento.setEstado(EstadoEvento.PENDIENTE_SUPLEMENTO);
                return toDto(eventoRepository.save(evento));
            }
            marcarUltimaNovedadResuelta(evento.getId(), EstadoNovedadEvento.APROBADO, null);
            EstadoEvento volver =
                    evento.getEstadoPrevioRevision() != null ? evento.getEstadoPrevioRevision() : EstadoEvento.ACTIVO;
            evento.setEstado(volver);
            evento.setEstadoPrevioRevision(null);
            evento.setResumenSolicitudEdicion(null);
            return toDto(eventoRepository.save(evento));
        }
        if (evento.getEstado() != EstadoEvento.PENDIENTE) {
            throw new CustomException(
                    "Solo se pueden aprobar eventos en estado PENDIENTE (alta nueva) o PENDIENTE_REVISION (cambios).",
                    HttpStatus.BAD_REQUEST);
        }
        evento.setEstado(EstadoEvento.APROBADO);
        evento.setResumenSolicitudEdicion(null);
        Evento guardado = eventoRepository.save(evento);
        if (guardado.getCosto() <= 0) {
            return activarYAcompanarOrganizador(guardado.getId());
        }
        return toDto(guardado);
    }

    @Override
    public EventoDTO rechazar(Long id, String motivo) {
        Evento evento = buscarPorId(id);
        if (evento.getEstado() == EstadoEvento.PENDIENTE_REVISION) {
            EstadoEvento volver =
                    evento.getEstadoPrevioRevision() != null ? evento.getEstadoPrevioRevision() : EstadoEvento.ACTIVO;
            try {
                revertirEventoDesdeUltimaNovedad(evento);
            } catch (Exception e) {
                throw new CustomException(
                        "No se pudo revertir la edición: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            marcarUltimaNovedadResuelta(evento.getId(), EstadoNovedadEvento.RECHAZADO, motivo);
            evento.setEstado(volver);
            evento.setEstadoPrevioRevision(null);
            evento.setResumenSolicitudEdicion(null);
            evento.setMotivoRechazo(motivo != null && !motivo.isBlank() ? motivo.trim() : null);
            return toDto(eventoRepository.save(evento));
        }
        if (evento.getEstado() != EstadoEvento.PENDIENTE) {
            throw new CustomException(
                    "Solo se pueden rechazar eventos en estado PENDIENTE (alta nueva) o PENDIENTE_REVISION (cambios).",
                    HttpStatus.BAD_REQUEST);
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
        if (motivo == null || motivo.isBlank()) {
            throw new CustomException("El motivo de cancelación es obligatorio.", HttpStatus.BAD_REQUEST);
        }
        if (evento.getEstado() == EstadoEvento.CANCELADO) {
            throw new CustomException("El evento ya está cancelado.", HttpStatus.BAD_REQUEST);
        }
        if (evento.getEstado() == EstadoEvento.FINALIZADO) {
            throw new CustomException("No se puede cancelar un evento ya FINALIZADO.", HttpStatus.BAD_REQUEST);
        }
        if (evento.getEstado() == EstadoEvento.PENDIENTE_CANCELACION) {
            throw new CustomException("Ya existe una solicitud de cancelación pendiente de revisión.", HttpStatus.CONFLICT);
        }

        if (evento.getEstado() == EstadoEvento.PENDIENTE) {
            evento.setEstado(EstadoEvento.CANCELADO);
            evento.setMotivoCancelacion(motivo.trim());
            evento.setEstadoPrevioRevision(null);
            Evento guardado = eventoRepository.save(evento);
            notificarInscritosCancelacion(guardado);
            return toDto(guardado);
        }

        if (evento.getEstado() == EstadoEvento.ACTIVO
                || evento.getEstado() == EstadoEvento.APROBADO
                || evento.getEstado() == EstadoEvento.PENDIENTE_REVISION
                || evento.getEstado() == EstadoEvento.PENDIENTE_SUPLEMENTO) {
            double valorPagado = pagoRepository.findByEventoId(id)
                    .filter((p) -> p.getEstado() == EstadoPago.APROBADO)
                    .map(Pago::getMonto)
                    .orElse(0.0);
            evento.setEstadoPrevioRevision(evento.getEstado());
            evento.setEstado(EstadoEvento.PENDIENTE_CANCELACION);
            evento.setMotivoCancelacion(motivo.trim());
            Evento guardado = eventoRepository.save(evento);
            registrarNovedadCancelacionSolicitud(guardado, organizadorId, valorPagado);
            return conAlerta(
                    guardado,
                    "La cancelación quedó pendiente de aprobación del administrador. Si se aprueba, solo se reembolsará el 70% del valor pagado a la plataforma.");
        }

        evento.setEstado(EstadoEvento.CANCELADO);
        evento.setMotivoCancelacion(motivo.trim());
        evento.setEstadoPrevioRevision(null);
        Evento guardado = eventoRepository.save(evento);
        notificarInscritosCancelacion(guardado);
        return toDto(guardado);
    }

    @Override
    public EventoDTO aprobarCancelacion(Long id) {
        Evento evento = buscarPorId(id);
        if (evento.getEstado() != EstadoEvento.PENDIENTE_CANCELACION) {
            throw new CustomException("Solo se puede aprobar una cancelación en estado PENDIENTE_CANCELACION.",
                    HttpStatus.BAD_REQUEST);
        }
        double valorPagado = pagoRepository.findByEventoId(id)
                .filter((p) -> p.getEstado() == EstadoPago.APROBADO)
                .map(Pago::getMonto)
                .orElse(0.0);
        marcarNovedadCancelacionAprobada(evento.getId());
        evento.setEstado(EstadoEvento.CANCELADO);
        evento.setEstadoPrevioRevision(null);
        Evento guardado = eventoRepository.save(evento);
        notificarInscritosCancelacion(guardado);
        notificacionService.crear(
                guardado.getOrganizadorId(),
                "Tu solicitud de cancelación fue aprobada. Valor pagado a la plataforma: " + copTexto(valorPagado)
                        + " COP. Monto orientativo a devolver (70%): " + copTexto(valorPagado * 0.70) + " COP.",
                TipoNotificacion.INFO);
        return toDto(guardado);
    }

    @Override
    public EventoDTO rechazarCancelacion(Long id, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new CustomException("El motivo de rechazo es obligatorio.", HttpStatus.BAD_REQUEST);
        }
        Evento evento = buscarPorId(id);
        if (evento.getEstado() != EstadoEvento.PENDIENTE_CANCELACION) {
            throw new CustomException("Solo se puede rechazar una cancelación en estado PENDIENTE_CANCELACION.",
                    HttpStatus.BAD_REQUEST);
        }
        marcarNovedadCancelacionRechazada(evento.getId(), motivo.trim());
        EstadoEvento volver =
                evento.getEstadoPrevioRevision() != null ? evento.getEstadoPrevioRevision() : EstadoEvento.ACTIVO;
        evento.setEstado(volver);
        evento.setEstadoPrevioRevision(null);
        evento.setMotivoCancelacion(null);
        Evento guardado = eventoRepository.save(evento);
        notificacionService.crear(
                guardado.getOrganizadorId(),
                "Tu solicitud de cancelación fue rechazada. Motivo: " + motivo.trim(),
                TipoNotificacion.ALERTA);
        return toDto(guardado);
    }

    private void notificarInscritosCancelacion(Evento guardado) {
        List<Inscripcion> inscripciones = inscripcionRepository.findByEventoId(guardado.getId());
        for (Inscripcion ins : inscripciones) {
            if (ins.getEstado() == EstadoInscripcion.CANCELADO) {
                continue;
            }
            notificacionService.crear(
                    ins.getUsuarioId(),
                    "El evento \"" + guardado.getNombre() + "\" fue cancelado por el organizador."
                            + (guardado.getMotivoCancelacion() != null
                                    ? " Motivo: " + guardado.getMotivoCancelacion()
                                    : ""),
                    TipoNotificacion.ALERTA);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EventoDTO activarPorPago(Long id) {
        Evento evento = buscarPorId(id);
        if (evento.getEstado() != EstadoEvento.APROBADO) {
            throw new CustomException("El evento debe estar APROBADO antes de activarse.", HttpStatus.BAD_REQUEST);
        }
        evento.setEstado(EstadoEvento.ACTIVO);
        return toDto(eventoRepository.save(evento));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EventoDTO activarTrasSuplementoPago(Long eventoId) {
        Evento evento = buscarPorId(eventoId);
        if (evento.getEstado() != EstadoEvento.PENDIENTE_SUPLEMENTO) {
            throw new CustomException(
                    "El evento no está pendiente de suplemento de pago.", HttpStatus.BAD_REQUEST);
        }
        EstadoEvento volver =
                evento.getEstadoPrevioRevision() != null ? evento.getEstadoPrevioRevision() : EstadoEvento.ACTIVO;
        evento.setEstado(volver);
        evento.setEstadoPrevioRevision(null);
        evento.setResumenSolicitudEdicion(null);
        marcarNovedadSuplementoAprobada(eventoId);
        return toDto(eventoRepository.save(evento));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resolverComplementoPagoSobreEventoActivo(Long eventoId) {
        Evento evento = buscarPorId(eventoId);
        if (evento.getEstado() != EstadoEvento.ACTIVO) {
            return;
        }
        marcarNovedadSuplementoAprobada(eventoId);
    }

    private void marcarNovedadSuplementoAprobada(Long eventoId) {
        eventoNovedadRepository
                .findFirstByEventoIdAndEstadoAndTipoOrderByFechaSolicitudDesc(
                        eventoId, EstadoNovedadEvento.PENDIENTE, TipoNovedadEvento.AUMENTO_HORAS)
                .ifPresent(n -> {
                    n.setEstado(EstadoNovedadEvento.APROBADO);
                    n.setFechaResolucion(LocalDateTime.now());
                    eventoNovedadRepository.save(n);
                });
    }

    private void marcarNovedadCancelacionAprobada(Long eventoId) {
        eventoNovedadRepository
                .findFirstByEventoIdAndEstadoAndTipoOrderByFechaSolicitudDesc(
                        eventoId, EstadoNovedadEvento.PENDIENTE, TipoNovedadEvento.CANCELACION_SOLICITUD)
                .ifPresent(n -> {
                    n.setEstado(EstadoNovedadEvento.APROBADO);
                    n.setFechaResolucion(LocalDateTime.now());
                    eventoNovedadRepository.save(n);
                });
    }

    private void marcarNovedadCancelacionRechazada(Long eventoId, String motivo) {
        eventoNovedadRepository
                .findFirstByEventoIdAndEstadoAndTipoOrderByFechaSolicitudDesc(
                        eventoId, EstadoNovedadEvento.PENDIENTE, TipoNovedadEvento.CANCELACION_SOLICITUD)
                .ifPresent(n -> {
                    n.setEstado(EstadoNovedadEvento.RECHAZADO);
                    n.setFechaResolucion(LocalDateTime.now());
                    n.setMotivoResolucion(motivo);
                    eventoNovedadRepository.save(n);
                });
    }

    @Override
    public List<EventoNovedadDTO> listarNovedades(Long eventoId) {
        return eventoNovedadRepository.findByEventoIdOrderByFechaSolicitudDesc(eventoId).stream()
                .map(this::toNovedadDto)
                .toList();
    }

    private EventoNovedadDTO toNovedadDto(EventoNovedad n) {
        EventoNovedadDTO d = new EventoNovedadDTO();
        d.setId(n.getId());
        d.setEventoId(n.getEventoId());
        d.setUsuarioSolicitanteId(n.getUsuarioSolicitanteId());
        d.setTipo(n.getTipo());
        d.setEstado(n.getEstado());
        d.setFechaSolicitud(n.getFechaSolicitud());
        d.setFechaResolucion(n.getFechaResolucion());
        d.setMotivoResolucion(n.getMotivoResolucion());
        d.setDetalleJson(n.getDetalleJson());
        return d;
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
    @Transactional(readOnly = true)
    public DisponibilidadSalonDTO consultarDisponibilidadSalon(
            String ubicacion,
            LocalDateTime desde,
            LocalDateTime hasta,
            Long excluirEventoId,
            LocalDateTime propuestaInicio,
            LocalDateTime propuestaFin) {
        if (desde == null || hasta == null) {
            throw new CustomException("Indica el rango de fechas (desde y hasta).", HttpStatus.BAD_REQUEST);
        }
        if (hasta.isBefore(desde)) {
            throw new CustomException("La fecha «hasta» no puede ser anterior a «desde».", HttpStatus.BAD_REQUEST);
        }
        String ub = ubicacionFinal(ubicacion);
        List<Evento> candidatos =
                eventoRepository.findByUbicacionNormalizadaYEstadoIn(ub, ESTADOS_RESERVAN_UBICACION);

        DisponibilidadSalonDTO resp = new DisponibilidadSalonDTO();
        resp.setUbicacion(ub);
        resp.setDesde(desde);
        resp.setHasta(hasta);

        List<FranjaOcupacionSalonDTO> franjas = new ArrayList<>();
        for (Evento e : candidatos) {
            if (excluirEventoId != null && excluirEventoId.equals(e.getId())) {
                continue;
            }
            LocalDateTime ini = e.getFecha();
            LocalDateTime fin = EventoVentanaUtil.instanteFin(e);
            if (fin.isBefore(desde) || ini.isAfter(hasta)) {
                continue;
            }
            FranjaOcupacionSalonDTO f = new FranjaOcupacionSalonDTO();
            f.setEventoId(e.getId());
            f.setNombreEvento(e.getNombre());
            f.setEstado(e.getEstado());
            f.setInicio(ini);
            f.setFin(fin);
            if (e.getOrganizadorId() != null) {
                usuarioRepository.findById(e.getOrganizadorId()).ifPresent(u -> f.setNombreOrganizador(u.getNombre()));
            }
            franjas.add(f);
        }
        franjas.sort((a, b) -> a.getInicio().compareTo(b.getInicio()));
        resp.setOcupaciones(franjas);

        if (propuestaInicio != null && propuestaFin != null) {
            if (!propuestaFin.isAfter(propuestaInicio)) {
                resp.setPropuestaDisponible(false);
                resp.setMensajePropuesta("La hora de fin debe ser posterior al inicio.");
            } else {
                boolean libre = true;
                StringBuilder conflicto = new StringBuilder();
                for (Evento otro : candidatos) {
                    if (excluirEventoId != null && excluirEventoId.equals(otro.getId())) {
                        continue;
                    }
                    if (EventoVentanaUtil.ventanasSeSolapan(propuestaInicio, propuestaFin, otro)) {
                        libre = false;
                        LocalDateTime otroFin = EventoVentanaUtil.instanteFin(otro);
                        conflicto.append(String.format(
                                Locale.ROOT,
                                "«%s» (%s – %s, %s). ",
                                otro.getNombre(),
                                otro.getFecha().format(FMT_VENTANA),
                                otroFin.format(FMT_VENTANA),
                                otro.getEstado()));
                    }
                }
                resp.setPropuestaDisponible(libre);
                resp.setMensajePropuesta(
                        libre
                                ? "El salón está libre en el horario indicado."
                                : "No disponible: ya hay evento(s) programado(s): " + conflicto);
            }
        }
        return resp;
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
        return EventoVentanaUtil.instanteFin(e);
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
        return eventoRepository.findByIdWithOrganizador(id)
                .orElseThrow(() -> new CustomException("No se encontró el evento con ID: " + id, HttpStatus.NOT_FOUND));
    }

    private EventoDTO toCatalogoPublicoDto(Evento evento) {
        EventoDTO dto = toDto(evento);
        dto.setCosto(null);
        dto.setAforoMaximo(null);
        dto.setAforoActual(null);
        dto.setOrganizadorId(null);
        dto.setOrganizadorNombre(null);
        dto.setOrganizadorEmail(null);
        return dto;
    }

    private EventoDTO toDto(Evento evento) {
        EventoDTO dto = modelMapper.map(evento, EventoDTO.class);
        Usuario org = evento.getOrganizador();
        if (org == null && evento.getOrganizadorId() != null) {
            org = usuarioRepository.findById(evento.getOrganizadorId()).orElse(null);
        }
        if (org != null) {
            dto.setOrganizadorNombre(org.getNombre());
            dto.setOrganizadorEmail(org.getEmail());
        }
        return dto;
    }
}
