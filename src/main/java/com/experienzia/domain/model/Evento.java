package com.experienzia.domain.model;

import com.experienzia.domain.exception.DatosInvalidosException;
import com.experienzia.domain.exception.UsuarioNoAutorizadoException;
import java.time.LocalDateTime;

public class Evento {
    
    private Long id;
    private String nombre;
    private String descripcion;
    private LocalDateTime fecha;
    private String ubicacion;
    private TipoEvento tipoEvento;
    private EstadoEvento estado;
    private int aforoMaximo;
    private int aforoActual;
    private double costo;
    private Long organizadorId;
    private String imagen;

    public Evento() {
    }

    public Evento(Long id, String nombre, String descripcion, LocalDateTime fecha, String ubicacion, 
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

    // --- Métodos de Reglas de Negocio (Rich Domain Model) ---

    /**
     * HU-007: Creación de evento.
     * Al crear, se validan datos y pasa a estado PENDIENTE.
     */
    public void crearEvento() {
        if (this.aforoMaximo <= 0) {
            throw new DatosInvalidosException("El aforo máximo debe ser mayor a 0.");
        }
        this.estado = EstadoEvento.PENDIENTE;
        this.aforoActual = 0; // Inicia sin asistentes
    }

    /**
     * HU-020: Aprobación de solicitudes de evento.
     */
    public void aprobarEvento() {
        if (this.estado != EstadoEvento.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden aprobar eventos que estén en estado PENDIENTE.");
        }
        this.estado = EstadoEvento.APROBADO;
    }

    /**
     * HU-020: Rechazo de solicitudes de evento.
     */
    public void rechazarEvento() {
        if (this.estado != EstadoEvento.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden rechazar eventos que estén en estado PENDIENTE.");
        }
        this.estado = EstadoEvento.RECHAZADO;
    }

    /**
     * HU-014: Activación por pago.
     * Solo los eventos aprobados pueden activarse (luego del pago).
     */
    public void activarPorPago() {
        if (this.estado != EstadoEvento.APROBADO) {
            throw new IllegalStateException("El evento debe estar APROBADO antes de poder activarse por pago.");
        }
        this.estado = EstadoEvento.ACTIVO;
    }

    /**
     * HU-009: Cancelación de evento.
     */
    public void cancelarEvento(Long userId) {
        validarOrganizador(userId);
        this.estado = EstadoEvento.CANCELADO;
    }

    /**
     * HU-008: Edición de evento.
     * Permite actualizar la información, validando reglas de aforo y propiedad.
     */
    public void actualizarEvento(Long userId, String nuevoNombre, String nuevaDescripcion, LocalDateTime nuevaFecha, 
                                 String nuevaUbicacion, int nuevoAforoMaximo, double nuevoCosto, String nuevaImagen) {
        validarOrganizador(userId);
        
        if (nuevoAforoMaximo <= 0) {
            throw new DatosInvalidosException("El aforo máximo debe ser mayor a 0.");
        }
        if (nuevoAforoMaximo < this.aforoActual) {
            throw new DatosInvalidosException("El aforo máximo no puede reducirse por debajo de la cantidad de asistentes actuales (" + this.aforoActual + ").");
        }

        this.nombre = nuevoNombre;
        this.descripcion = nuevaDescripcion;
        this.fecha = nuevaFecha;
        this.ubicacion = nuevaUbicacion;
        this.aforoMaximo = nuevoAforoMaximo;
        this.costo = nuevoCosto;
        this.imagen = nuevaImagen;
    }

    /**
     * Incrementa el aforo del evento tras un registro exitoso.
     */
    public void incrementarAforo() {
        validarAforo();
        this.aforoActual++;
    }

    /**
     * Valida que aún haya cupos disponibles.
     */
    public void validarAforo() {
        if (this.aforoActual >= this.aforoMaximo) {
            throw new IllegalStateException("El evento ha alcanzado su aforo máximo.");
        }
    }

    /**
     * Método auxiliar de seguridad.
     */
    private void validarOrganizador(Long userId) {
        if (this.organizadorId == null || !this.organizadorId.equals(userId)) {
            throw new UsuarioNoAutorizadoException("Acción denegada: Solo el organizador del evento tiene permisos.");
        }
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
