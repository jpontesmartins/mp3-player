package com.mp3player.lyrics.infrastructure;

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
