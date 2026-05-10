package com.experienzia.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        // Estrategia estricta para evitar ambigüedades cuando una entidad tiene
        // tanto el id (Long) como la relación @ManyToOne (objeto) con el mismo nombre.
        mapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setSkipNullEnabled(true)
                .setFieldMatchingEnabled(false)
                .setAmbiguityIgnored(true);
        return mapper;
    }
}
