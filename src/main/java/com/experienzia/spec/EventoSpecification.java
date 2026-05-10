package com.experienzia.spec;

import com.experienzia.entity.EstadoEvento;
import com.experienzia.entity.Evento;
import com.experienzia.entity.TipoEvento;
import lombok.Data;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class EventoSpecification {

    @Data
    public static class EventoSearchCriteria {
        private String nombre;
        private String categoria;
        private String tipoEvento;
        private String estado;
        private LocalDateTime fechaDesde;
        private LocalDateTime fechaHasta;
        private Long organizadorId;
    }

    public static Specification<Evento> hasNombre(String nombre) {
        return (root, query, cb) -> nombre == null || nombre.isBlank()
                ? null
                : cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%");
    }

    public static Specification<Evento> hasCategoria(String categoria) {
        return (root, query, cb) -> categoria == null || categoria.isBlank()
                ? null
                : cb.like(cb.lower(root.get("categoria")), "%" + categoria.toLowerCase() + "%");
    }

    public static Specification<Evento> hasTipo(String tipo) {
        return (root, query, cb) -> tipo == null || tipo.isBlank()
                ? null
                : cb.equal(root.get("tipoEvento"), TipoEvento.valueOf(tipo.toUpperCase()));
    }

    public static Specification<Evento> hasEstado(String estado) {
        return (root, query, cb) -> estado == null || estado.isBlank()
                ? null
                : cb.equal(root.get("estado"), EstadoEvento.valueOf(estado.toUpperCase()));
    }

    public static Specification<Evento> fechaDesde(LocalDateTime desde) {
        return (root, query, cb) -> desde == null
                ? null
                : cb.greaterThanOrEqualTo(root.get("fecha"), desde);
    }

    public static Specification<Evento> fechaHasta(LocalDateTime hasta) {
        return (root, query, cb) -> hasta == null
                ? null
                : cb.lessThanOrEqualTo(root.get("fecha"), hasta);
    }

    public static Specification<Evento> hasOrganizador(Long organizadorId) {
        return (root, query, cb) -> organizadorId == null
                ? null
                : cb.equal(root.get("organizadorId"), organizadorId);
    }
}
