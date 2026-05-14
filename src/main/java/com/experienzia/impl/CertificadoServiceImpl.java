package com.experienzia.impl;

import com.experienzia.dto.CertificadoDTO;
import com.experienzia.entity.Certificado;
import com.experienzia.entity.EstadoInscripcion;
import com.experienzia.entity.Evento;
import com.experienzia.entity.Inscripcion;
import com.experienzia.entity.Usuario;
import com.experienzia.exceptions.CustomException;
import com.experienzia.repository.CertificadoRepository;
import com.experienzia.repository.EventoRepository;
import com.experienzia.repository.InscripcionRepository;
import com.experienzia.repository.UsuarioRepository;
import com.experienzia.service.CertificadoService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CertificadoServiceImpl implements CertificadoService {

    private final CertificadoRepository certificadoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EventoRepository eventoRepository;
    private final ModelMapper modelMapper;

    public CertificadoServiceImpl(CertificadoRepository certificadoRepository,
                                  InscripcionRepository inscripcionRepository,
                                  UsuarioRepository usuarioRepository,
                                  EventoRepository eventoRepository,
                                  ModelMapper modelMapper) {
        this.certificadoRepository = certificadoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventoRepository = eventoRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public CertificadoDTO generar(Long inscripcionId) {
        Inscripcion ins = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new CustomException("Inscripción no encontrada.", HttpStatus.NOT_FOUND));
        if (ins.getEstado() != EstadoInscripcion.ASISTIO) {
            throw new CustomException("El certificado solo puede generarse para usuarios que hayan asistido al evento.",
                    HttpStatus.BAD_REQUEST);
        }
        if (certificadoRepository.findByUsuarioIdAndEventoId(ins.getUsuarioId(), ins.getEventoId()).isPresent()) {
            throw new CustomException("Ya existe un certificado generado para esta asistencia.", HttpStatus.CONFLICT);
        }
        return toDto(crearCertificado(ins.getUsuarioId(), ins.getEventoId()));
    }

    @Override
    public List<CertificadoDTO> generarMasivoPorEvento(Long eventoId, Long organizadorId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new CustomException("Evento no encontrado.", HttpStatus.NOT_FOUND));
        if (organizadorId != null && evento.getOrganizadorId() != null
                && !evento.getOrganizadorId().equals(organizadorId)) {
            throw new CustomException("Solo el organizador del evento puede generar certificados masivos.",
                    HttpStatus.FORBIDDEN);
        }
        List<Inscripcion> inscripciones = inscripcionRepository.findByEventoId(eventoId);
        List<CertificadoDTO> resultado = new ArrayList<>();
        for (Inscripcion ins : inscripciones) {
            if (ins.getEstado() != EstadoInscripcion.ASISTIO) continue;
            if (certificadoRepository.findByUsuarioIdAndEventoId(ins.getUsuarioId(), ins.getEventoId()).isPresent()) {
                continue;
            }
            resultado.add(toDto(crearCertificado(ins.getUsuarioId(), ins.getEventoId())));
        }
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificadoDTO> listarPorUsuario(Long usuarioId) {
        return certificadoRepository.findByUsuarioId(usuarioId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificadoDTO> listarPorEvento(Long eventoId) {
        return certificadoRepository.findByEventoId(eventoId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CertificadoDTO validarPorCodigo(String codigoUnico) {
        return certificadoRepository.findByCodigoUnico(codigoUnico)
                .map(this::toDto)
                .orElseThrow(() -> new CustomException("Certificado no válido o no encontrado.", HttpStatus.NOT_FOUND));
    }

    private Certificado crearCertificado(Long usuarioId, Long eventoId) {
        Certificado c = new Certificado();
        c.setUsuarioId(usuarioId);
        c.setEventoId(eventoId);
        c.setFechaGeneracion(LocalDateTime.now());
        c.setCodigoUnico(UUID.randomUUID().toString());
        return certificadoRepository.save(c);
    }

    private CertificadoDTO toDto(Certificado c) {
        CertificadoDTO dto = modelMapper.map(c, CertificadoDTO.class);
        Usuario u = usuarioRepository.findById(c.getUsuarioId()).orElse(null);
        if (u != null) {
            dto.setNombreAsistente(u.getNombre());
            dto.setNumeroDocumento(u.getNumeroDocumento());
        }
        Evento ev = eventoRepository.findById(c.getEventoId()).orElse(null);
        if (ev != null) {
            dto.setNombreEvento(ev.getNombre());
            dto.setFechaEvento(ev.getFecha());
            dto.setDuracionHoras(ev.getDuracionHoras());
            if (ev.getUbicacion() != null && !ev.getUbicacion().isBlank()) {
                dto.setCiudadExpedicion(ev.getUbicacion().trim());
            }
            if (ev.getOrganizadorId() != null) {
                usuarioRepository.findById(ev.getOrganizadorId())
                        .ifPresent(org -> dto.setNombreOrganizador(org.getNombre()));
            }
        }
        return dto;
    }
}
