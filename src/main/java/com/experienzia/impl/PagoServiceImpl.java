package com.experienzia.impl;

import com.experienzia.dto.PagoDTO;
import com.experienzia.entity.EstadoPago;
import com.experienzia.entity.Evento;
import com.experienzia.entity.Pago;
import com.experienzia.entity.Rol;
import com.experienzia.entity.TipoNotificacion;
import com.experienzia.entity.Usuario;
import com.experienzia.exceptions.CustomException;
import com.experienzia.repository.EventoRepository;
import com.experienzia.repository.PagoRepository;
import com.experienzia.repository.UsuarioRepository;
import com.experienzia.service.AuditoriaService;
import com.experienzia.service.EventoService;
import com.experienzia.service.FileStorageService;
import com.experienzia.service.InscripcionService;
import com.experienzia.service.NotificacionService;
import com.experienzia.service.PagoService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servicio de pagos. El pago lo realiza siempre el ORGANIZADOR del evento
 * para activar su evento (tarifa de la plataforma = duracionHoras * precioPorHora).
 * Los asistentes y el staff NO interactúan con los pagos.
 */
@Service
@Transactional
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final FileStorageService fileStorageService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;
    private final EventoService eventoService;
    private final InscripcionService inscripcionService;
    private final ModelMapper modelMapper;

    public PagoServiceImpl(PagoRepository pagoRepository,
                           EventoRepository eventoRepository,
                           UsuarioRepository usuarioRepository,
                           FileStorageService fileStorageService,
                           NotificacionService notificacionService,
                           AuditoriaService auditoriaService,
                           EventoService eventoService,
                           InscripcionService inscripcionService,
                           ModelMapper modelMapper) {
        this.pagoRepository = pagoRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.fileStorageService = fileStorageService;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
        this.eventoService = eventoService;
        this.inscripcionService = inscripcionService;
        this.modelMapper = modelMapper;
    }

    @Override
    public PagoDTO registrar(Long eventoId, Long organizadorId, MultipartFile archivo, String direccionIp) {
        if (eventoId == null) {
            throw new CustomException("El evento es requerido.", HttpStatus.BAD_REQUEST);
        }
        if (organizadorId == null) {
            throw new CustomException("El organizador es requerido.", HttpStatus.BAD_REQUEST);
        }

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new CustomException("El evento no existe.", HttpStatus.NOT_FOUND));

        Usuario organizador = usuarioRepository.findById(organizadorId)
                .orElseThrow(() -> new CustomException("El usuario no existe.", HttpStatus.NOT_FOUND));

        // Solo el ORGANIZADOR dueño del evento puede pagar.
        if (organizador.getRol() != Rol.ORGANIZADOR) {
            throw new CustomException("Solo los organizadores pueden registrar pagos de eventos.",
                    HttpStatus.FORBIDDEN);
        }
        if (evento.getOrganizadorId() == null
                || !evento.getOrganizadorId().equals(organizadorId)) {
            throw new CustomException("Solo el organizador dueño del evento puede pagar la tarifa.",
                    HttpStatus.FORBIDDEN);
        }

        Optional<Pago> pagoOpt = pagoRepository.findByEventoId(eventoId);

        // Flujo correcto: comprobante cuando el evento ya fue aprobado por admin (APROBADO)
        // o cuando hay suplemento de pago por horas adicionales (PENDIENTE_SUPLEMENTO).
        // Caso defensivo: estado ACTIVO pero fila de pago en complemento pendiente (delta + saldo).
        boolean complementoPendienteEnActivo =
                evento.getEstado() == com.experienzia.entity.EstadoEvento.ACTIVO
                        && pagoOpt.isPresent()
                        && pagoOpt.get().getEstado() == EstadoPago.PENDIENTE
                        && pagoOpt.get().getSaldoAprobadoPrevio() != null
                        && pagoOpt.get().getSaldoAprobadoPrevio() > 0;

        if (evento.getEstado() != com.experienzia.entity.EstadoEvento.APROBADO
                && evento.getEstado() != com.experienzia.entity.EstadoEvento.PENDIENTE_SUPLEMENTO
                && !complementoPendienteEnActivo) {
            throw new CustomException(
                    "Solo se puede subir el comprobante cuando el evento está APROBADO por el administrador "
                            + "o en estado PENDIENTE_SUPLEMENTO (pago adicional por más horas). "
                            + "Estado actual: " + evento.getEstado() + ".",
                    HttpStatus.BAD_REQUEST);
        }
        if (evento.getCosto() <= 0) {
            throw new CustomException(
                    "Este evento no tiene tarifa de activación (costo $0). No debes subir comprobante; "
                            + "debería activarse solo al ser aprobado por el administrador.",
                    HttpStatus.BAD_REQUEST);
        }

        Optional<Pago> existente = pagoOpt;
        if (existente.isPresent()) {
            EstadoPago est = existente.get().getEstado();
            if (est == EstadoPago.APROBADO) {
                throw new CustomException(
                        "Ya existe un pago aprobado para este evento.",
                        HttpStatus.CONFLICT);
            }
            if (est == EstadoPago.PENDIENTE || est == EstadoPago.RECHAZADO) {
                Pago ex = existente.get();
                String viejo = ex.getComprobanteUrl();
                if (viejo != null && !viejo.isBlank()) {
                    fileStorageService.borrarComprobantePublico(viejo);
                }
                String comprobanteUrl = fileStorageService.guardarComprobante(archivo);
                ex.setComprobanteUrl(comprobanteUrl);
                ex.setEstado(EstadoPago.PENDIENTE);
                ex.setMotivoRechazo(null);
                ex.setFecha(LocalDateTime.now());
                if (ex.getSaldoAprobadoPrevio() == null) {
                    ex.setMonto(evento.getCosto());
                }
                Pago guardado = pagoRepository.save(ex);
                auditoriaService.registrar(organizadorId, "PAGO_REGISTRADO", "Pago", guardado.getId(), direccionIp);
                return toDto(guardado);
            }
        }

        // El monto se toma directamente del costo calculado del evento.
        double monto = evento.getCosto();

        String comprobanteUrl = fileStorageService.guardarComprobante(archivo);
        Pago pago = new Pago();
        pago.setEventoId(eventoId);
        pago.setOrganizadorId(organizadorId);
        pago.setComprobanteUrl(comprobanteUrl);
        pago.setMonto(monto);
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setFecha(LocalDateTime.now());
        Pago guardado = pagoRepository.save(pago);

        auditoriaService.registrar(organizadorId, "PAGO_REGISTRADO", "Pago", guardado.getId(), direccionIp);
        return toDto(guardado);
    }

    @Override
    public PagoDTO aprobar(Long pagoId, Long aprobadorId, String direccionIp) {
        assertAdministradorPuedeGestionarPagos(aprobadorId);
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new CustomException("El pago no existe.", HttpStatus.NOT_FOUND));
        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            throw new CustomException("Solo se pueden aprobar pagos PENDIENTES.", HttpStatus.BAD_REQUEST);
        }
        if (pago.getComprobanteUrl() == null || pago.getComprobanteUrl().isBlank()) {
            throw new CustomException(
                    "No hay comprobante adjunto. El organizador debe subir el archivo del pago "
                            + "(o del complemento por horas adicionales) antes de que puedas aprobar.",
                    HttpStatus.BAD_REQUEST);
        }
        pago.setEstado(EstadoPago.APROBADO);
        pago.setMotivoRechazo(null);
        pago.setAprobadorId(aprobadorId);
        pago.setFechaResolucion(LocalDateTime.now());

        /** Comprobante de complemento (delta sobre monto ya aprobado); el evento puede seguir ACTIVO u otro estado. */
        AtomicBoolean teniaSaldoComplemento = new AtomicBoolean(false);
        eventoRepository.findById(pago.getEventoId()).ifPresent(ev -> {
            double total = ev.getCosto();
            if (pago.getSaldoAprobadoPrevio() != null) {
                teniaSaldoComplemento.set(true);
                pago.setMonto(pago.getSaldoAprobadoPrevio() + pago.getMonto());
                pago.setSaldoAprobadoPrevio(null);
            } else if (Math.abs(pago.getMonto() - total) > 0.02) {
                pago.setMonto(total);
            }
        });

        Pago guardado = pagoRepository.save(pago);

        // Notificar al organizador, activar el evento y auto-inscribirlo como
        // asistente principal de su propio evento (PASO 6).
        notificacionService.crear(guardado.getOrganizadorId(),
                "Tu pago de la tarifa fue aprobado. Tu evento ha sido activado.",
                TipoNotificacion.INFO);
        try {
            Evento ev = eventoRepository.findById(guardado.getEventoId()).orElse(null);
            if (ev == null) {
                // no-op
            } else if (ev.getEstado() == com.experienzia.entity.EstadoEvento.PENDIENTE_SUPLEMENTO) {
                eventoService.activarTrasSuplementoPago(guardado.getEventoId());
            } else if (ev.getEstado() == com.experienzia.entity.EstadoEvento.APROBADO) {
                eventoService.activarPorPago(guardado.getEventoId());
            } else if (teniaSaldoComplemento.get()) {
                // Complemento aprobado: no llamar activarPorPago (exige APROBADO); evita marcar la TX rollback-only.
                eventoService.resolverComplementoPagoSobreEventoActivo(guardado.getEventoId());
            }
            inscripcionService.inscribirOrganizadorEnSuEvento(guardado.getEventoId());
        } catch (RuntimeException ex) {
            // El pago ya quedó APROBADO; no revertir si activación o inscripción del organizador falla por estado/cupo.
        }

        auditoriaService.registrar(aprobadorId, "PAGO_APROBADO", "Pago", guardado.getId(), direccionIp);
        return toDto(guardado);
    }

    @Override
    public PagoDTO rechazar(Long pagoId, String motivo, Long aprobadorId, String direccionIp) {
        if (motivo == null || motivo.isBlank()) {
            throw new CustomException("El motivo de rechazo es obligatorio.", HttpStatus.BAD_REQUEST);
        }
        assertAdministradorPuedeGestionarPagos(aprobadorId);
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new CustomException("El pago no existe.", HttpStatus.NOT_FOUND));
        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            throw new CustomException("Solo se pueden rechazar pagos PENDIENTES.", HttpStatus.BAD_REQUEST);
        }
        if (pago.getSaldoAprobadoPrevio() != null) {
            // Rechazo del comprobante del suplemento: el organizador debe subir otro; el pago sigue PENDIENTE.
            String viejo = pago.getComprobanteUrl();
            if (viejo != null && !viejo.isBlank()) {
                fileStorageService.borrarComprobantePublico(viejo);
            }
            pago.setComprobanteUrl(null);
            pago.setMotivoRechazo(motivo.trim());
            pago.setAprobadorId(aprobadorId);
            pago.setFechaResolucion(LocalDateTime.now());
            Pago guardadoSup = pagoRepository.save(pago);
            notificacionService.crear(guardadoSup.getOrganizadorId(),
                    "El comprobante del pago adicional fue rechazado. Motivo: " + motivo.trim()
                            + ". Sube un nuevo comprobante por la diferencia pendiente.",
                    TipoNotificacion.ALERTA);
            auditoriaService.registrar(aprobadorId, "PAGO_RECHAZADO", "Pago", guardadoSup.getId(), direccionIp);
            return toDto(guardadoSup);
        }

        pago.setEstado(EstadoPago.RECHAZADO);
        pago.setMotivoRechazo(motivo.trim());
        pago.setAprobadorId(aprobadorId);
        pago.setFechaResolucion(LocalDateTime.now());

        Pago guardado = pagoRepository.save(pago);

        notificacionService.crear(guardado.getOrganizadorId(),
                "Tu pago fue rechazado. Motivo: " + guardado.getMotivoRechazo()
                        + ". Sube un nuevo comprobante desde tu panel.",
                TipoNotificacion.ALERTA);

        auditoriaService.registrar(aprobadorId, "PAGO_RECHAZADO", "Pago", guardado.getId(), direccionIp);
        return toDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoDTO> listarPendientes() {
        return pagoRepository.findByEstado(EstadoPago.PENDIENTE).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoDTO> listarTodos() {
        return pagoRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoDTO> listarPorOrganizador(Long organizadorId) {
        return pagoRepository.findByOrganizadorId(organizadorId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PagoDTO> obtenerPorEvento(Long eventoId) {
        if (eventoId == null) {
            return Optional.empty();
        }
        return pagoRepository.findByEventoId(eventoId).map(this::toDto);
    }

    private PagoDTO toDto(Pago pago) {
        PagoDTO dto = modelMapper.map(pago, PagoDTO.class);
        // Enriquecemos con datos del evento y del organizador para que el admin
        // pueda ver la información directamente sin más llamadas.
        eventoRepository.findById(pago.getEventoId()).ifPresent(ev -> {
            dto.setNombreEvento(ev.getNombre());
            dto.setFechaEvento(ev.getFecha());
        });
        usuarioRepository.findById(pago.getOrganizadorId()).ifPresent(u -> {
            dto.setNombreOrganizador(u.getNombre());
            dto.setEmailOrganizador(u.getEmail());
        });
        dto.setSaldoAprobadoPrevio(pago.getSaldoAprobadoPrevio());
        return dto;
    }

    private void assertAdministradorPuedeGestionarPagos(Long aprobadorId) {
        if (aprobadorId == null) {
            throw new CustomException(
                    "Debes indicar el identificador del administrador que aprueba o rechaza el pago (aprobadorId).",
                    HttpStatus.BAD_REQUEST);
        }
        Usuario u = usuarioRepository
                .findById(aprobadorId)
                .orElseThrow(() -> new CustomException("El usuario aprobador no existe.", HttpStatus.NOT_FOUND));
        if (u.getRol() != Rol.ADMIN) {
            throw new CustomException(
                    "Solo un usuario con rol ADMINISTRADOR puede aprobar o rechazar pagos de eventos.",
                    HttpStatus.FORBIDDEN);
        }
        if (u.getEstado() != com.experienzia.entity.Estado.ACTIVO) {
            throw new CustomException(
                    "La cuenta del administrador no está activa; no puede gestionar pagos.",
                    HttpStatus.FORBIDDEN);
        }
    }
}
