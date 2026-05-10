package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecuperarPasswordResponseDTO {
    private Long usuarioId;
    private String email;
    private String passwordTemporal;
    private String mensaje;
}
