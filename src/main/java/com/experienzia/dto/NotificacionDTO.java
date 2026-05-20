package com.experienzia.dto;

import com.experienzia.entity.TipoNotificacion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Notificacion que ve el usuario en su panel.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class NotificacionDTO {
    /** Identificador unico del registro. */
    private Long id;
    /** Id del usuario relacionado. */
    private Long usuarioId;
    /** Texto del mensaje o notificacion. */
    private String mensaje;
    /** Tipo o categoria auxiliar. */
    private TipoNotificacion tipo;
    /** Si la notificacion ya fue leida. */
    private boolean leida;
    /** Fecha y hora (inicio o generacion). */
    private LocalDateTime fecha;
}
