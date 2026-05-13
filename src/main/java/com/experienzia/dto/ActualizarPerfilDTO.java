package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarPerfilDTO {
    /** Cadena vacía borra el teléfono; null no modifica el valor guardado. */
    private String telefono;
    /** Si va vacío/null, no cambia la contraseña. */
    private String nuevaPassword;
}
