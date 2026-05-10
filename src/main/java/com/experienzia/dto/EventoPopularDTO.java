package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoPopularDTO {
    private Long eventoId;
    private String nombre;
    private Long totalInscritos;
}
