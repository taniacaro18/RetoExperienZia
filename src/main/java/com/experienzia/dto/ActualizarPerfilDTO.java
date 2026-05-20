package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Campos que el usuario puede editar en su perfil.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class ActualizarPerfilDTO {
    /** Cadena vacía borra el teléfono; null no modifica el valor guardado. */
    private String telefono;
    /** Si va vacío/null, no cambia la contraseña. */
    private String nuevaPassword;
}
