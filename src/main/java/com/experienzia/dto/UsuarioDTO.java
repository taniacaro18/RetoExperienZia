package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {

    private Long id;
    private String nombre;
    private String email;
    private String password;
    private String telefono;
    private String tipoDocumento;
    private String numeroDocumento;
    /** ASISTENTE, ORGANIZADOR, STAFF, ADMIN. */
    private String rol;
    /** ACTIVO, PENDIENTE, RECHAZADO, INACTIVO. */
    private String estado;
    /** Solo se devuelve cuando rol = STAFF. */
    private Long organizadorId;
    /** Usado en registro público para distinguir entre ASISTENTE y ORGANIZADOR. */
    private String tipo;
}
