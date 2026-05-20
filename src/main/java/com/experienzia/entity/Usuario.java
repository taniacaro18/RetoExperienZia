package com.experienzia.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Representa la tabla "usuarios" en la base de datos.
 * Guarda los datos de cada persona que usa ExperienZia (nombre, correo, contraseña, etc.).
 * Sirve para iniciar sesión, saber si alguien es asistente, organizador, staff o administrador,
 * y relacionar inscripciones, eventos y pagos con la persona correcta.
 */
@Entity
@Table(name = "usuarios")
@Data
public class Usuario {

    // Identificador único de cada usuario (número que genera la base de datos)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre completo o de perfil de la persona
    @Column(nullable = false, length = 150)
    private String nombre;

    // Correo electrónico; se usa para entrar a la app y debe ser único
    @Column(unique = true, nullable = false, length = 150)
    private String email;

    // Contraseña guardada de forma segura (encriptada en la práctica)
    @Column(nullable = false, length = 200)
    private String password;

    // Teléfono de contacto (opcional)
    @Column(unique = true, length = 50)
    private String telefono;

    // Tipo de documento, por ejemplo CC o CE (opcional)
    @Column(length = 30)
    private String tipoDocumento;

    // Número del documento de identidad (opcional y único)
    @Column(unique = true, length = 50)
    private String numeroDocumento;

    // Rol del usuario en la app: asistente, organizador, staff o admin
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Rol rol;

    // Si la cuenta está activa, pendiente de aprobación, rechazada o inactiva
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Estado estado;

    // Número del organizador que creó este usuario STAFF; vacío para los demás roles
    @Column(name = "organizador_id")
    private Long organizadorId;

    // Llave foránea hacia la tabla usuarios: el organizador dueño de este STAFF (solo lectura en BD)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizador_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_usuario_organizador"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario organizador;
}
