package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Email para recuperar contrasena.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class RecuperarPasswordDTO {
    /** Correo electronico del usuario. */
    private String email;
    /** Numero del documento de identidad. */
    private String numeroDocumento;
}
