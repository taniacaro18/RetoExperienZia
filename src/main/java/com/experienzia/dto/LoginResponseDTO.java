package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Respuesta del login con token y datos del usuario.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class LoginResponseDTO {

    /** JWT para Authorization: Bearer … */
    private String accessToken;

    /** Campo usuario. */
    private UsuarioDTO usuario;
}
