package com.experienzia.repository;

import com.experienzia.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la tabla de usuarios.
 * Spring Data genera las consultas basicas (save, findById, etc.).
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    /** Busca un usuario por su email (login). */
    Optional<Usuario> findByEmail(String email);

    /** Verifica si ya existe ese email (para no duplicar en registro). */
    boolean existsByEmail(String email);

    /** Verifica si ya existe ese numero de documento. */
    boolean existsByNumeroDocumento(String numeroDocumento);

    /** Verifica si ya existe ese telefono. */
    boolean existsByTelefono(String telefono);
}
