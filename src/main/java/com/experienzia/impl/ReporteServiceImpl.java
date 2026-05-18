package com.experienzia.impl;

import com.experienzia.dto.AsistenciaDTO;
import com.experienzia.dto.AsistenteEventoDTO;
import com.experienzia.dto.CurvaIngresoPuntoDTO;
import com.experienzia.dto.DashboardAdminDTO;
import com.experienzia.dto.DashboardOrganizadorDTO;
import com.experienzia.dto.DesempenoStaffDTO;
import com.experienzia.dto.EventoPopularDTO;
import com.experienzia.dto.PuntoSerieDTO;
import com.experienzia.dto.ReporteEventoAvanzadoDTO;
import com.experienzia.dto.ReporteEventoDTO;
import com.experienzia.dto.ResumenDTO;
import com.experienzia.entity.Auditoria;
import com.experienzia.entity.Estado;
import com.experienzia.entity.EstadoInscripcion;
import com.experienzia.entity.Evento;
import com.experienzia.entity.FuncionStaff;
import com.experienzia.entity.Inscripcion;
import com.experienzia.entity.Rol;
import com.experienzia.entity.StaffEventoAsignacion;
import com.experienzia.entity.Usuario;
import com.experienzia.exceptions.CustomException;
import com.experienzia.repository.AuditoriaRepository;
import com.experienzia.repository.EventoRepository;
import com.experienzia.repository.InscripcionRepository;
import com.experienzia.repository.StaffEventoAsignacionRepository;
import com.experienzia.repository.UsuarioRepository;
import com.experienzia.service.ReporteService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReporteServiceImpl implements ReporteService {

    private static final List<String> ACCIONES_CHECK_IN_QR = List.of("CHECK_IN_QR");
    private static final List<String> ACCIONES_CHECK_IN_MANUAL = List.of("CHECK_IN");

    private final InscripcionRepository inscripcionRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final StaffEventoAsignacionRepository staffEventoRepository;
    private final AuditoriaRepository auditoriaRepository;

    public ReporteServiceImpl(InscripcionRepository inscripcionRepository,
                              EventoRepository eventoRepository,
                              UsuarioRepository usuarioRepository,
                              StaffEventoAsignacionRepository staffEventoRepository,
                              AuditoriaRepository auditoriaRepository) {
        this.inscripcionRepository = inscripcionRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.staffEventoRepository = staffEventoRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    @Override
    public List<EventoPopularDTO> obtenerEventosPopulares() {
        return inscripcionRepository.findEventosPopulares().stream().map(obj -> {
            Long eventoId = (Long) obj[0];
            Long totalInscritos = (Long) obj[1];
            String nombre = eventoRepository.findById(eventoId)
                    .map(Evento::getNombre)
                    .orElse("Evento Desconocido");
            return new EventoPopularDTO(eventoId, nombre, totalInscritos);
        }).toList();
    }

    @Override
    public AsistenciaDTO obtenerAsistenciaPorEvento(Long eventoId) {
        long totalAsistieron = inscripcionRepository.countByEventoIdAndEstado(eventoId, EstadoInscripcion.ASISTIO);
        return new AsistenciaDTO(eventoId, totalAsistieron);
    }

    @Override
    public List<Long> obtenerUsuariosPorEvento(Long eventoId) {
        return inscripcionRepository.findUsuarioIdsByEventoId(eventoId);
    }

    @Override
    public ResumenDTO obtenerResumenGeneral() {
        return new ResumenDTO(
                usuarioRepository.count(),
                eventoRepository.count(),
                inscripcionRepository.count());
    }

    @Override
    public ReporteEventoDTO obtenerReporteDetalladoEvento(Long eventoId, Long organizadorId) {
        Evento evento = buscarEventoYValidarOrganizador(eventoId, organizadorId);

        List<Inscripcion> inscripciones = inscripcionRepository.findByEventoId(eventoId);
        long inscritos = 0L;
        long asistencias = 0L;
        long enSala = 0L;
        List<AsistenteEventoDTO> asistentes = new ArrayList<>();
        for (Inscripcion ins : inscripciones) {
            if (ins.getEstado() == EstadoInscripcion.CANCELADO) continue;
            inscritos++;
            if (ins.getEstado() == EstadoInscripcion.ASISTIO) {
                asistencias++;
                if (ins.getFechaCheckOut() == null) enSala++;
            }
            Usuario u = usuarioRepository.findById(ins.getUsuarioId()).orElse(null);
            if (u != null) {
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
                asistentes.add(dto);
            }
        }

        if (inscritos == 0 && asistencias == 0) {
            throw new CustomException("No hay datos suficientes para generar el reporte de este evento.",
                    HttpStatus.NOT_FOUND);
        }

        double porcentajeOcupacion = evento.getAforoMaximo() == 0
                ? 0.0
                : ((double) asistencias / evento.getAforoMaximo()) * 100.0;
        double porcentajeAsistencia = inscritos == 0
                ? 0.0
                : ((double) asistencias / inscritos) * 100.0;

        ReporteEventoDTO r = new ReporteEventoDTO();
        r.setEventoId(evento.getId());
        r.setNombreEvento(evento.getNombre());
        r.setEstadoEvento(evento.getEstado() != null ? evento.getEstado().name() : null);
        r.setFechaEvento(evento.getFecha());
        r.setDuracionHoras(evento.getDuracionHoras());
        r.setAforoMaximo(evento.getAforoMaximo());
        r.setInscritos(inscritos);
        r.setAsistenciasReales(asistencias);
        r.setAsistentesActualmenteEnSala(enSala);
        r.setPorcentajeOcupacion(redondear(porcentajeOcupacion));
        r.setPorcentajeAsistenciaSobreInscritos(redondear(porcentajeAsistencia));
        r.setAsistentes(asistentes);
        return r;
    }

    @Override
    public ReporteEventoAvanzadoDTO obtenerReporteAvanzadoEvento(Long eventoId, Long organizadorId) {
        Evento evento = buscarEventoYValidarOrganizador(eventoId, organizadorId);

        List<Inscripcion> inscripciones = inscripcionRepository.findByEventoId(eventoId);
        long inscritos = 0L;
        long asistieron = 0L;
        Map<Integer, long[]> porHora = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) porHora.put(h, new long[]{0L, 0L});

        long checkOuts = 0L;
        for (Inscripcion ins : inscripciones) {
            if (ins.getEstado() == EstadoInscripcion.CANCELADO) continue;
            inscritos++;
            if (ins.getFechaCheckIn() != null) {
                asistieron++;
                int h = ins.getFechaCheckIn().getHour();
                porHora.get(h)[0]++;
            }
            if (ins.getFechaCheckOut() != null) {
                checkOuts++;
                int h = ins.getFechaCheckOut().getHour();
                porHora.get(h)[1]++;
            }
        }

        // Desglose QR vs manual desde la auditoría (acción del registro de check-in).
        // Filtramos por cada inscripción del evento.
        long checkInsQR = inscripciones.stream()
                .map(Inscripcion::getId)
                .mapToLong(insId -> auditoriaRepository
                        .findByEntidadAndEntidadIdAndAccionInOrderByFechaDesc("Inscripcion", insId, ACCIONES_CHECK_IN_QR)
                        .size())
                .sum();
        long checkInsManuales = inscripciones.stream()
                .map(Inscripcion::getId)
                .mapToLong(insId -> auditoriaRepository
                        .findByEntidadAndEntidadIdAndAccionInOrderByFechaDesc("Inscripcion", insId, ACCIONES_CHECK_IN_MANUAL)
                        .size())
                .sum();

        // Desempeño por staff (a partir de las asignaciones del evento + auditorías filtradas por staff).
        List<StaffEventoAsignacion> asignaciones = staffEventoRepository.findByEventoId(eventoId);
        List<DesempenoStaffDTO> desempeno = new ArrayList<>();
        for (StaffEventoAsignacion a : asignaciones) {
            Usuario staff = usuarioRepository.findById(a.getStaffUsuarioId()).orElse(null);
            DesempenoStaffDTO d = new DesempenoStaffDTO();
            d.setStaffUsuarioId(a.getStaffUsuarioId());
            d.setNombre(staff != null ? staff.getNombre() : null);
            d.setFuncion(a.getFuncion() == null ? FuncionStaff.GENERAL.name() : a.getFuncion().name());

            List<Auditoria> registros = auditoriaRepository
                    .findByUsuarioIdOrderByFechaDesc(a.getStaffUsuarioId());
            long sIn = 0, sInQr = 0, sInMan = 0, sOut = 0;
            for (Auditoria au : registros) {
                if (!"Inscripcion".equals(au.getEntidad())) continue;
                Inscripcion ins = inscripcionRepository.findById(au.getEntidadId()).orElse(null);
                if (ins == null || !ins.getEventoId().equals(eventoId)) continue;
                String accion = au.getAccion();
                if ("CHECK_IN".equals(accion)) { sIn++; sInMan++; }
                else if ("CHECK_IN_QR".equals(accion)) { sIn++; sInQr++; }
                else if ("CHECK_OUT".equals(accion) || "CHECK_OUT_QR".equals(accion)) { sOut++; }
            }
            d.setCheckInsRegistrados(sIn);
            d.setCheckOutsRegistrados(sOut);
            d.setCheckInsPorQR(sInQr);
            d.setCheckInsManuales(sInMan);
            desempeno.add(d);
        }

        List<CurvaIngresoPuntoDTO> curva = new ArrayList<>();
        for (var entry : porHora.entrySet()) {
            CurvaIngresoPuntoDTO p = new CurvaIngresoPuntoDTO();
            p.setHora(entry.getKey());
            p.setIngresos(entry.getValue()[0]);
            p.setSalidas(entry.getValue()[1]);
            curva.add(p);
        }

        long faltaron = Math.max(0L, inscritos - asistieron);
        double porcentajeOcupacion = evento.getAforoMaximo() == 0
                ? 0.0 : ((double) asistieron / evento.getAforoMaximo()) * 100.0;
        double porcentajeAsistencia = inscritos == 0
                ? 0.0 : ((double) asistieron / inscritos) * 100.0;

        ReporteEventoAvanzadoDTO r = new ReporteEventoAvanzadoDTO();
        r.setEventoId(evento.getId());
        r.setNombreEvento(evento.getNombre());
        r.setFechaEvento(evento.getFecha());
        r.setAforoMaximo(evento.getAforoMaximo());
        r.setInscritos(inscritos);
        r.setAsistieron(asistieron);
        r.setFaltaron(faltaron);
        r.setPorcentajeOcupacion(redondear(porcentajeOcupacion));
        r.setPorcentajeAsistencia(redondear(porcentajeAsistencia));
        r.setCheckInsTotal(asistieron);
        r.setCheckInsPorQR(checkInsQR);
        r.setCheckInsManuales(checkInsManuales);
        r.setCheckOutsTotal(checkOuts);
        r.setCurvaIngreso(curva);
        r.setDesempenoStaff(desempeno);
        return r;
    }

    @Override
    public DashboardOrganizadorDTO obtenerDashboardOrganizador(Long organizadorId) {
        if (organizadorId == null) {
            throw new CustomException("organizadorId es obligatorio.", HttpStatus.BAD_REQUEST);
        }
        Usuario org = usuarioRepository.findById(organizadorId)
                .orElseThrow(() -> new CustomException("Organizador no encontrado.", HttpStatus.NOT_FOUND));
        if (org.getRol() != Rol.ORGANIZADOR) {
            throw new CustomException("El usuario no es ORGANIZADOR.", HttpStatus.FORBIDDEN);
        }

        List<Evento> eventos = eventoRepository.findByOrganizadorId(organizadorId);
        long activos = 0, pendientes = 0, cancelados = 0, cuposActivos = 0;
        for (Evento e : eventos) {
            switch (e.getEstado()) {
                case ACTIVO -> {
                    activos++;
                    cuposActivos += e.getAforoActual();
                }
                case PENDIENTE -> pendientes++;
                case CANCELADO -> cancelados++;
                default -> { /* no-op */ }
            }
        }

        long totalInscritos = 0L;
        long asistencias30 = 0L;
        LocalDateTime hace30 = LocalDateTime.now().minusDays(30);
        Map<YearMonth, Long> inscripcionesPorMes = nuevaSerieMensualVacia();
        Map<YearMonth, Long> eventosPorMes = nuevaSerieMensualVacia();

        for (Evento e : eventos) {
            // mes de creación del evento ≈ mes de la fecha del evento (proxy).
            YearMonth ymEvt = YearMonth.from(e.getFecha());
            eventosPorMes.computeIfPresent(ymEvt, (k, v) -> v + 1);

            List<Inscripcion> ins = inscripcionRepository.findByEventoId(e.getId());
            for (Inscripcion i : ins) {
                if (i.getEstado() == EstadoInscripcion.CANCELADO) continue;
                totalInscritos++;
                YearMonth ym = YearMonth.from(i.getFechaInscripcion());
                inscripcionesPorMes.computeIfPresent(ym, (k, v) -> v + 1);
                if (i.getFechaCheckIn() != null && i.getFechaCheckIn().isAfter(hace30)) {
                    asistencias30++;
                }
            }
        }

        DashboardOrganizadorDTO dto = new DashboardOrganizadorDTO();
        dto.setOrganizadorId(organizadorId);
        dto.setEventosActivos(activos);
        dto.setEventosPendientes(pendientes);
        dto.setEventosCancelados(cancelados);
        dto.setEventosTotales(eventos.size());
        dto.setTotalInscritos(totalInscritos);
        dto.setAforoMaximoPorEvento(600);
        dto.setCuposOcupadosEventosActivos(cuposActivos);
        dto.setAsistenciasUltimos30Dias(asistencias30);
        dto.setSerieMensualEventos(toSerie(eventosPorMes));
        dto.setSerieMensualInscripciones(toSerie(inscripcionesPorMes));
        return dto;
    }

    @Override
    public DashboardAdminDTO obtenerDashboardAdmin() {
        List<Evento> eventos = eventoRepository.findAll();
        long activos = 0, pendientes = 0, cancelados = 0;
        Map<YearMonth, Long> eventosPorMes = nuevaSerieMensualVacia();
        for (Evento e : eventos) {
            switch (e.getEstado()) {
                case ACTIVO -> activos++;
                case PENDIENTE -> pendientes++;
                case CANCELADO -> cancelados++;
                default -> { /* no-op */ }
            }
            YearMonth ym = YearMonth.from(e.getFecha());
            eventosPorMes.computeIfPresent(ym, (k, v) -> v + 1);
        }

        List<Usuario> usuarios = usuarioRepository.findAll();
        long usuariosActivos = 0, usuariosPendientes = 0, organizadoresActivos = 0;
        long asistentes = 0, staff = 0;
        Map<YearMonth, Long> usuariosPorMes = nuevaSerieMensualVacia();
        for (Usuario u : usuarios) {
            if (u.getEstado() == Estado.ACTIVO) usuariosActivos++;
            else if (u.getEstado() == Estado.PENDIENTE) usuariosPendientes++;

            if (u.getRol() == Rol.ORGANIZADOR && u.getEstado() == Estado.ACTIVO) organizadoresActivos++;
            if (u.getRol() == Rol.ASISTENTE) asistentes++;
            if (u.getRol() == Rol.STAFF) staff++;
            // No tenemos createdAt → como proxy, usamos el id como orden temporal: cae en este mes.
            // Hasta no tener fechaCreacion en Usuario, dejamos los conteos en el mes actual.
        }
        // Sin fecha de creación de usuario, ponemos todos en el mes actual como proxy.
        usuariosPorMes.computeIfPresent(YearMonth.now(), (k, v) -> v + usuarios.size());

        DashboardAdminDTO dto = new DashboardAdminDTO();
        dto.setEventosActivos(activos);
        dto.setEventosPendientes(pendientes);
        dto.setEventosCancelados(cancelados);
        dto.setEventosTotales(eventos.size());
        dto.setUsuariosTotales(usuarios.size());
        dto.setUsuariosActivos(usuariosActivos);
        dto.setUsuariosPendientes(usuariosPendientes);
        dto.setOrganizadoresActivos(organizadoresActivos);
        dto.setAsistentesTotales(asistentes);
        dto.setStaffTotales(staff);
        dto.setInscripcionesTotales(inscripcionRepository.count());
        dto.setSerieMensualEventos(toSerie(eventosPorMes));
        dto.setSerieMensualUsuarios(toSerie(usuariosPorMes));
        return dto;
    }

    private Evento buscarEventoYValidarOrganizador(Long eventoId, Long organizadorId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new CustomException("Evento no encontrado.", HttpStatus.NOT_FOUND));
        if (organizadorId != null && evento.getOrganizadorId() != null
                && !evento.getOrganizadorId().equals(organizadorId)) {
            throw new CustomException("Solo el organizador del evento puede ver este reporte.", HttpStatus.FORBIDDEN);
        }
        return evento;
    }

    /**
     * Genera un mapa con los últimos 12 meses (incluyendo el actual) inicializados en 0.
     */
    private static Map<YearMonth, Long> nuevaSerieMensualVacia() {
        Map<YearMonth, Long> map = new LinkedHashMap<>();
        YearMonth ahora = YearMonth.now();
        for (int i = 11; i >= 0; i--) {
            map.put(ahora.minusMonths(i), 0L);
        }
        return map;
    }

    private static List<PuntoSerieDTO> toSerie(Map<YearMonth, Long> map) {
        List<PuntoSerieDTO> serie = new ArrayList<>();
        for (var e : map.entrySet()) {
            serie.add(new PuntoSerieDTO(e.getKey().toString(), e.getValue()));
        }
        return serie;
    }

    private static double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
