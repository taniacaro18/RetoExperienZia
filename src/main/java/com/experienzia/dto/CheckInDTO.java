package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInDTO {
    private Long staffUsuarioId;
    private String codigoQR;
    private Long eventoId;
}
