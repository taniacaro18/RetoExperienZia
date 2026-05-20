package com.experienzia.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configura Jackson (JSON) como bean de Spring.
 * Lo necesitamos para convertir objetos a JSON en servicios como EventoServiceImpl.
 */
@Configuration
public class JacksonConfig {

    /** Crea el ObjectMapper principal de la aplicacion. */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }
}
