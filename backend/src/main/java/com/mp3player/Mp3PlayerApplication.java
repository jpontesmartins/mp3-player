package com.mp3player;

import com.mp3player.config.CoverProperties;
import com.mp3player.config.LyricsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Classe principal da aplicação Spring Boot.
 */
@SpringBootApplication
@EnableConfigurationProperties({LyricsProperties.class, CoverProperties.class})
public class Mp3PlayerApplication {

    public static void main(String[] args) {
        SpringApplication.run(Mp3PlayerApplication.class, args);
    }
}