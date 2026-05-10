package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearStaffDTO {
    private String nombre;
    private String email;
    private String password;
    private String telefono;
    private String tipoDocumento;
    private String numeroDocumento;
    private Long organizadorId;
}
