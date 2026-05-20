package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Datos que manda el usuario para iniciar sesion.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class LoginDTO {
    /** Correo electronico del usuario. */
    private String email;
    /** Contrasena (solo se usa al registrar o login). */
    private String password;
}
