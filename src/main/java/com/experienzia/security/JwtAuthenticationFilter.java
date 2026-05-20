package com.experienzia.security;

import com.experienzia.entity.Estado;
import com.experienzia.entity.Rol;
import com.experienzia.entity.Usuario;
import com.experienzia.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import jakarta.annotation.Nonnull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro de Spring Security que intercepta cada peticion HTTP.
 * Si viene el header Authorization con Bearer token, valida el JWT y carga el usuario.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    /** No filtramos OPTIONS ni rutas publicas (login, swagger, etc.). */
    @Override
    protected boolean shouldNotFilter(@Nonnull HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || SecurityPaths.isPublic(request.getRequestURI());
    }

    /** Aqui validamos el token y ponemos al usuario en el contexto de seguridad. */
    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();
        try {
            Long userId = jwtService.extractUserId(token);
            Usuario usuario = usuarioRepository.findById(userId).orElse(null);
            if (usuario == null || usuario.getEstado() != Estado.ACTIVO) {
                filterChain.doFilter(request, response);
                return;
            }
            String rol = usuario.getRol() != null ? usuario.getRol().name() : Rol.ASISTENTE.name();
            var auth = new UsernamePasswordAuthenticationToken(
                    usuario.getId(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
