package com.mp3player.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigTest {

    @Test
    void corsConfigurerReturnsNonNull() {
        WebMvcConfigurer configurer = new CorsConfig().corsConfigurer();
        assertNotNull(configurer);
    }

    @Test
    void addCorsMappingsDoesNotThrow() {
        WebMvcConfigurer configurer = new CorsConfig().corsConfigurer();
        CorsRegistry registry = new CorsRegistry();
        assertDoesNotThrow(() -> configurer.addCorsMappings(registry));
    }

    @Test
    void beanReturnsWebMvcConfigurer() {
        CorsConfig config = new CorsConfig();
        assertTrue(config.corsConfigurer() instanceof WebMvcConfigurer);
    }
}
