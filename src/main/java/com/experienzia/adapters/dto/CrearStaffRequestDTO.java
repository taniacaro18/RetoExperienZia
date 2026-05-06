package com.experienzia.adapters.dto;

/**
 * DTO de entrada para que un ORGANIZADOR cree un usuario STAFF.
 * No incluye el campo "tipo" porque el rol siempre será STAFF.
 */
public class CrearStaffRequestDTO {

    private String nombre;
    private String email;
    private String password;
    private String telefono;
    private String tipoDocumento;
    private String numeroDocumento;
    /**
     * ID del organizador que realiza la petición.
     * En un sistema con autenticación real esto vendría del token JWT;
     * aquí se recibe en el cuerpo para mantener la demo sin Spring Security.
     */
    private Long organizadorId;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

    public Long getOrganizadorId() { return organizadorId; }
    public void setOrganizadorId(Long organizadorId) { this.organizadorId = organizadorId; }
}
