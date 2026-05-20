package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Datos de un usuario (registro, perfil, listados).
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class UsuarioDTO {

    /** Identificador unico del registro. */
    private Long id;
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
    /** ASISTENTE, ORGANIZADOR, STAFF, ADMIN. */
    private String rol;
    /** ACTIVO, PENDIENTE, RECHAZADO, INACTIVO. */
    private String estado;
    /** Solo se devuelve cuando rol = STAFF. */
    private Long organizadorId;
    /** Usado en registro público para distinguir entre ASISTENTE y ORGANIZADOR. */
    private String tipo;
}
