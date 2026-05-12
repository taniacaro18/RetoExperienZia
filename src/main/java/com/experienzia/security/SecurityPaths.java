package com.experienzia.security;

/**
 * Rutas HTTP públicas (deben coincidir con {@code SecurityFilterChain}).
 */
public final class SecurityPaths {

    private SecurityPaths() {
    }

    public static boolean isPublic(String uri) {
        if (uri == null) {
            return false;
        }
        if (uri.startsWith("/uploads/")) {
            return true;
        }
        if (uri.startsWith("/actuator/health") || uri.startsWith("/actuator/info")) {
            return true;
        }
        if (uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-ui")) {
            return true;
        }
        if ("/swagger-ui.html".equals(uri)) {
            return true;
        }
        if (uri.startsWith("/api/certificados/validar/") || uri.startsWith("/api/certificados/pdf/")) {
            return true;
        }
        if (uri.startsWith("/api/eventos/catalogo/publicos")) {
            return true;
        }
        return uri.startsWith("/api/usuarios/login")
                || uri.startsWith("/api/usuarios/registro")
                || uri.startsWith("/api/usuarios/recuperar");
    }
}
