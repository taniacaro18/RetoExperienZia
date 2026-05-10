package com.experienzia.impl;

import com.experienzia.dto.NotificacionDTO;
import com.experienzia.entity.Notificacion;
import com.experienzia.entity.TipoNotificacion;
import com.experienzia.exceptions.CustomException;
import com.experienzia.repository.NotificacionRepository;
import com.experienzia.service.NotificacionService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final ModelMapper modelMapper;

    public NotificacionServiceImpl(NotificacionRepository notificacionRepository, ModelMapper modelMapper) {
        this.notificacionRepository = notificacionRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public NotificacionDTO crear(Long usuarioId, String mensaje, TipoNotificacion tipo) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(usuarioId);
        notificacion.setMensaje(mensaje);
        notificacion.setTipo(tipo == null ? TipoNotificacion.INFO : tipo);
        notificacion.setLeida(false);
        notificacion.setFecha(LocalDateTime.now());
        return modelMapper.map(notificacionRepository.save(notificacion), NotificacionDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificacionDTO> listarPorUsuario(Long usuarioId) {
        return notificacionRepository.findByUsuarioIdOrderByFechaDesc(usuarioId).stream()
                .map(n -> modelMapper.map(n, NotificacionDTO.class))
                .toList();
    }

    @Override
    public NotificacionDTO marcarLeida(Long id) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new CustomException("Notificación no encontrada.", HttpStatus.NOT_FOUND));
        notificacion.setLeida(true);
        return modelMapper.map(notificacionRepository.save(notificacion), NotificacionDTO.class);
    }
}
