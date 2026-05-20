package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos del staff asignado a un evento, incluyendo su función específica.
 * Se usa en GET /api/eventos/{id}/staff (para que el organizador vea quién hace qué).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Staff asignado a un evento con su funcion.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class StaffAsignadoDTO {
    /** Campo asignacion id. */
    private Long asignacionId;
    /** Id del usuario con rol STAFF. */
    private Long staffUsuarioId;
    /** Nombre (del evento, usuario, etc.). */
    private String nombre;
    /** Correo electronico del usuario. */
    private String email;
    /** Numero de telefono de contacto. */
    private String telefono;
    /** Campo estado usuario. */
    private String estadoUsuario;
    /** Funcion del staff en el evento (check-in, etc.). */
    private String funcion;
}
