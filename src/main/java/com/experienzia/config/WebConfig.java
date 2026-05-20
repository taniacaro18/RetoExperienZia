package com.experienzia.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuracion web: sirve archivos estaticos de /uploads/** desde la carpeta uploads/.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Mapea la carpeta uploads/ del disco a la URL publica /uploads/** */
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
