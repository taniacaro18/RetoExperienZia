package com.experienzia.infrastructure.persistence.entity;

import com.experienzia.domain.model.EstadoEvento;
import com.experienzia.domain.model.TipoEvento;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventos")
public class EventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String descripcion;

    private LocalDateTime fecha;

    private String ubicacion;

    @Enumerated(EnumType.STRING)
    private TipoEvento tipoEvento;

    @Enumerated(EnumType.STRING)
    private EstadoEvento estado;

    private int aforoMaximo;

    private int aforoActual;

    private double costo;

    private Long organizadorId;

    private String imagen;

    public EventoEntity() {
    }

    public EventoEntity(Long id, String nombre, String descripcion, LocalDateTime fecha, String ubicacion, 
                        TipoEvento tipoEvento, EstadoEvento estado, int aforoMaximo, int aforoActual, 
                        double costo, Long organizadorId, String imagen) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.ubicacion = ubicacion;
        this.tipoEvento = tipoEvento;
        this.estado = estado;
        this.aforoMaximo = aforoMaximo;
        this.aforoActual = aforoActual;
        this.costo = costo;
        this.organizadorId = organizadorId;
        this.imagen = imagen;
    }

    // --- Getters y Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public TipoEvento getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEvento tipoEvento) { this.tipoEvento = tipoEvento; }

    public EstadoEvento getEstado() { return estado; }
    public void setEstado(EstadoEvento estado) { this.estado = estado; }

    public int getAforoMaximo() { return aforoMaximo; }
    public void setAforoMaximo(int aforoMaximo) { this.aforoMaximo = aforoMaximo; }

    public int getAforoActual() { return aforoActual; }
    public void setAforoActual(int aforoActual) { this.aforoActual = aforoActual; }

    public double getCosto() { return costo; }
    public void setCosto(double costo) { this.costo = costo; }

    public Long getOrganizadorId() { return organizadorId; }
    public void setOrganizadorId(Long organizadorId) { this.organizadorId = organizadorId; }

    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
}
