package com.mp3player.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mp3.cover")
public record CoverProperties(
        String userAgent,
        String itunesUrl,
        String deezerUrl,
        int timeoutConnect,
        int timeoutDownload
) {
}
