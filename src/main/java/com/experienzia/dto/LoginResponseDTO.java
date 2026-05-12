package com.experienzia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    /** JWT para Authorization: Bearer … */
    private String accessToken;

    private UsuarioDTO usuario;
}
