package com.mp3player.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração de CORS para permitir chamadas de origens externas (necessário
 * para o frontend em desenvolvimento no Vite).
 */
@Configuration
public class CorsConfig {

    /**
     * Configura o CORS para aceitar requisições de qualquer origem, método e cabeçalho.
     *
     * @return configurador de CORS para o Spring MVC
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("*")
                        .allowedHeaders("*");
            }
        };
    }
}
