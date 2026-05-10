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
public class StaffAsignadoDTO {
    private Long asignacionId;
    private Long staffUsuarioId;
    private String nombre;
    private String email;
    private String telefono;
    private String estadoUsuario;
    private String funcion;
}
