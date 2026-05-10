package com.experienzia.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI experienziaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ExperienZia API")
                        .description("API REST del sistema de gestión de eventos ExperienZia. "
                                + "Cubre las historias de usuario HU-001 a HU-024 (gestión de usuarios, eventos, "
                                + "inscripciones, check-in/out, pagos, certificados, reportes y auditoría).")
                        .version("0.0.1-SNAPSHOT")
                        .contact(new Contact().name("Equipo ExperienZia"))
                        .license(new License().name("Uso académico")))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentación e historias de usuario")
                        .url("/swagger-ui.html"));
    }
}
