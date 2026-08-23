package com.mp3player.metadata.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de configuração para busca e download de capas de álbuns.
 *
 * @param userAgent User-Agent used in HTTP requests
 * @param itunesUrl URL base da API de busca do iTunes
 * @param deezerUrl URL base da API de busca do Deezer
 * @param timeoutConnect timeout de conexão em milissegundos
 * @param timeoutDownload timeout de download em milissegundos
 */
@ConfigurationProperties(prefix = "mp3.cover")
public record CoverProperties(
        String userAgent,
        String itunesUrl,
        String deezerUrl,
        int timeoutConnect,
        int timeoutDownload
) {
}
