package com.experienzia.adapters.dto;

import com.experienzia.domain.model.Usuario;

public class UsuarioDtoMapper {

    public static Usuario toDomain(UsuarioRequestDTO dto) {
        if (dto == null) return null;
        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setEmail(dto.getEmail());
        usuario.setPassword(dto.getPassword());
        usuario.setTelefono(dto.getTelefono());
        usuario.setTipoDocumento(dto.getTipoDocumento());
        usuario.setNumeroDocumento(dto.getNumeroDocumento());
        return usuario;
    }

    public static UsuarioResponseDTO toDto(Usuario usuario) {
        if (usuario == null) return null;
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombre());
        dto.setEmail(usuario.getEmail());
        dto.setTelefono(usuario.getTelefono());
        dto.setTipoDocumento(usuario.getTipoDocumento());
        dto.setNumeroDocumento(usuario.getNumeroDocumento());
        
        if (usuario.getRol() != null) {
            dto.setRol(usuario.getRol().name());
        }
        
        if (usuario.getEstado() != null) {
            dto.setEstado(usuario.getEstado().name());
        }
        
        return dto;
    }
}
