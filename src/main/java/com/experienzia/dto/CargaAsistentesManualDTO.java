package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CargaAsistentesManualDTO {
    private Long organizadorId;
    private List<FilaAsistenteCargaDTO> filas;
}
