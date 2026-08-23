package com.mp3player.lyrics.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de configuração do módulo de letras, mapeadas a partir de
 * {@code mp3.lyrics.*} no {@code application.properties}.
 *
 * @param letras configurações da fonte letras.mus.br
 */
@ConfigurationProperties(prefix = "mp3.lyrics")
public record LyricsProperties(
        Letras letras
) {
    /**
     * Configurações específicas da fonte letras.mus.br.
     *
     * @param baseUrl URL base do site
     * @param userAgent User-Agent utilizado nas requisições HTTP
     * @param searchPath path do endpoint de busca no site
     * @param timeoutConnect timeout de conexão em milissegundos
     * @param timeoutFetch timeout de busca em milissegundos
     * @param enabled se a fonte está habilitada
     * @param priority prioridade da fonte (menor = mais prioritário)
     */
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
