package com.experienzia.domain.model;

public class Usuario {
    private Long id;
    private String nombre;
    private String email;
    private String password;
    private String telefono;
    private String tipoDocumento;
    private String numeroDocumento;
    private Rol rol;
    private Estado estado;

    public Usuario() {
    }

    public Usuario(Long id, String nombre, String email, String password, String telefono,
                   String tipoDocumento, String numeroDocumento, Rol rol, Estado estado) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.telefono = telefono;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.rol = rol;
        this.estado = estado;
    }

    // --- Métodos de Negocio (basados en Historias de Usuario) ---

    /**
     * HU-001: Registro de asistentes
     * Registra automáticamente al usuario como Asistente y lo activa.
     */
    public void asignarRolAsistente() {
        this.rol = Rol.ASISTENTE;
        this.estado = Estado.ACTIVO;
    }

    /**
     * HU-002: Solicitud Registro de Organizadores
     * Registra al usuario como Organizador y lo deja en estado Pendiente
     * hasta que un administrador lo apruebe.
     */
    public void marcarOrganizadorPendiente() {
        this.rol = Rol.ORGANIZADOR;
        this.estado = Estado.PENDIENTE;
    }

    /**
     * Activa la cuenta del usuario (usado por el administrador para 
     * aprobar organizadores u otros flujos).
     */
    public void activarUsuario() {
        this.estado = Estado.ACTIVO;
    }

    // --- Getters y Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }
}
