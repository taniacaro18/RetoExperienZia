package com.experienzia.adapters.outbound;

import com.experienzia.domain.model.Usuario;
import com.experienzia.domain.port.UsuarioRepository;
import com.experienzia.infrastructure.persistence.UsuarioJpaRepository;
import com.experienzia.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class UsuarioRepositoryImpl implements UsuarioRepository {

    private final UsuarioJpaRepository usuarioJpaRepository;

    public UsuarioRepositoryImpl(UsuarioJpaRepository usuarioJpaRepository) {
        this.usuarioJpaRepository = usuarioJpaRepository;
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioEntity entity = toEntity(usuario);
        UsuarioEntity savedEntity = usuarioJpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioJpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existePorEmail(String email) {
        return usuarioJpaRepository.existsByEmail(email);
    }

    @Override
    public List<Usuario> listarTodos() {
        return usuarioJpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void eliminar(Long id) {
        usuarioJpaRepository.deleteById(id);
    }

    // --- Mappers ---

    private UsuarioEntity toEntity(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioEntity(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getPassword(),
                usuario.getTelefono(),
                usuario.getTipoDocumento(),
                usuario.getNumeroDocumento(),
                usuario.getRol(),
                usuario.getEstado(),
                usuario.getOrganizadorId()
        );
    }

    private Usuario toDomain(UsuarioEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Usuario(
                entity.getId(),
                entity.getNombre(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getTelefono(),
                entity.getTipoDocumento(),
                entity.getNumeroDocumento(),
                entity.getRol(),
                entity.getEstado(),
                entity.getOrganizadorId()
        );
    }
}
