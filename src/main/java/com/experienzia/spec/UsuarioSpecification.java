package com.experienzia.spec;

import com.experienzia.entity.Estado;
import com.experienzia.entity.Rol;
import com.experienzia.entity.Usuario;
import lombok.Data;
import org.springframework.data.jpa.domain.Specification;

public class UsuarioSpecification {

    @Data
    public static class UsuarioSearchCriteria {
        private String nombre;
        private String email;
        private String rol;
        private String estado;
        private Long organizadorId;
    }

    public static Specification<Usuario> hasNombre(String nombre) {
        return (root, query, cb) -> nombre == null || nombre.isEmpty()
                ? null
                : cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%");
    }

    public static Specification<Usuario> hasEmail(String email) {
        return (root, query, cb) -> email == null || email.isEmpty()
                ? null
                : cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
    }

    public static Specification<Usuario> hasRol(String rol) {
        return (root, query, cb) -> rol == null || rol.isEmpty()
                ? null
                : cb.equal(root.get("rol"), Rol.valueOf(rol.toUpperCase()));
    }

    public static Specification<Usuario> hasEstado(String estado) {
        return (root, query, cb) -> estado == null || estado.isEmpty()
                ? null
                : cb.equal(root.get("estado"), Estado.valueOf(estado.toUpperCase()));
    }

    public static Specification<Usuario> hasOrganizadorId(Long organizadorId) {
        return (root, query, cb) -> organizadorId == null
                ? null
                : cb.equal(root.get("organizadorId"), organizadorId);
    }
}
