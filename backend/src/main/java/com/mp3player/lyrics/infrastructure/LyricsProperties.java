package com.mp3player.lyrics.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mp3.lyrics")
public record LyricsProperties(
        Letras letras
) {
    public record Letras(
            String baseUrl,
            String userAgent,
            String searchPath,
            int timeoutConnect,
            int timeoutFetch,
            boolean enabled,
            int priority
    ) {}
}
