package com.experienzia.security;

/**
 * Lista de rutas HTTP que no necesitan token JWT (login, registro, swagger, etc.).
 * Debe coincidir con lo configurado en SecurityConfig.
 */
public final class SecurityPaths {

    private SecurityPaths() {
    }

    /** Devuelve true si la URI es publica y no pide autenticacion. */
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
