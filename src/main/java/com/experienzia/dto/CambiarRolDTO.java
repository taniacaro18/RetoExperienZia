package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Nuevo rol que asigna el admin.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class CambiarRolDTO {
    /** Rol del usuario en el sistema. */
    private String rol;
}
