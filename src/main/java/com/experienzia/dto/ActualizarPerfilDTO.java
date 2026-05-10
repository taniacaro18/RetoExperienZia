package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarPerfilDTO {
    private String nombre;
    private String email;
    private String telefono;
    private String tipoDocumento;
    private String numeroDocumento;
    /** Si va vacío/null, no cambia. */
    private String nuevaPassword;
}
