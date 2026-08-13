package com.mp3player.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mp3.lyrics")
public record LyricsProperties(
        String baseUrl,
        String userAgent,
        String searchPath,
        int timeoutConnect,
        int timeoutFetch
) {
}
