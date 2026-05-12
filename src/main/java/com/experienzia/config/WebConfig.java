package com.experienzia.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuración global del módulo Web:
 *  - CORS lo define {@link com.experienzia.config.SecurityConfig} (filtro unificado con JWT).
 *  - Recursos estáticos para servir los comprobantes subidos por el organizador
 *    en /uploads/** desde el directorio físico ./uploads/ del proyecto.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Exponemos el directorio físico "uploads/" como ruta pública "/uploads/**"
        // para que las imágenes/PDFs del comprobante de pago puedan visualizarse.
        Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
        String location = uploadDir.toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
