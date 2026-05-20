package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Una fila de asistente para carga masiva.
 * Lo usamos en los controllers para no exponer las entidades directamente.
 */
public class FilaAsistenteCargaDTO {
    /** Nombre (del evento, usuario, etc.). */
    private String nombre;
    /** Correo electronico del usuario. */
    private String email;
    /** Numero de telefono de contacto. */
    private String telefono;
    /** Tipo de documento (CC, CE, etc.). */
    private String tipoDocumento;
    /** Numero del documento de identidad. */
    private String numeroDocumento;
}
