package com.mp3player.lyrics.infrastructure;

import com.mp3player.lyrics.domain.port.LyricsSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuração que cria as fontes de letras habilitadas via
 * {@code application.properties}.
 *
 * <p>Exemplo de configuração:</p>
 * <pre>
 * mp3.lyrics.letras.enabled=true
 * mp3.lyrics.letras.priority=1
 * mp3.lyrics.letras.base-url=https://www.letras.mus.br
 * mp3.lyrics.letras.user-agent=Mozilla/5.0
 * mp3.lyrics.letras.search-path=/
 * mp3.lyrics.letras.timeout-connect=8000
 * mp3.lyrics.letras.timeout-fetch=15000
 * </pre>
 */
@Configuration
@EnableConfigurationProperties(LyricsProperties.class)
public class LyricsConfig {

    /**
     * Cria a lista de fontes de letras habilitadas a partir das propriedades configuradas.
     *
     * @param props propriedades de configuração do módulo de letras
     * @return lista de {@link LyricsSource} habilitadas e ordenadas por prioridade
     */
    @Bean
    public List<LyricsSource> lyricsSources(LyricsProperties props) {
        List<LyricsSource> sources = new ArrayList<>();

        sources.add(new LetrasMusBrSource(
                props.letras().baseUrl(),
                props.letras().userAgent(),
                props.letras().timeoutConnect(),
                props.letras().timeoutFetch(),
                props.letras().searchPath(),
                props.letras().enabled(),
                props.letras().priority()
        ));

        // Adicione novas fontes aqui:
        // sources.add(new GeniusSource(...));
        // sources.add(new MusixmatchSource(...));

        return sources;
    }
}
