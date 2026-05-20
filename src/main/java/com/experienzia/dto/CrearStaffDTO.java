package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Datos para crear un usuario STAFF.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class CrearStaffDTO {
    /** Nombre (del evento, usuario, etc.). */
    private String nombre;
    /** Correo electronico del usuario. */
    private String email;
    /** Contrasena (solo se usa al registrar o login). */
    private String password;
    /** Numero de telefono de contacto. */
    private String telefono;
    /** Tipo de documento (CC, CE, etc.). */
    private String tipoDocumento;
    /** Numero del documento de identidad. */
    private String numeroDocumento;
    /** Id del organizador dueño del evento o staff. */
    private Long organizadorId;
}
