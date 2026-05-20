package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Respuesta con contrasena temporal.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class RecuperarPasswordResponseDTO {
    /** Id del usuario relacionado. */
    private Long usuarioId;
    /** Correo electronico del usuario. */
    private String email;
    /** Contrasena temporal generada. */
    private String passwordTemporal;
    /** Texto del mensaje o notificacion. */
    private String mensaje;
}
