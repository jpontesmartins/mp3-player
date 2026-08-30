package com.mp3player.config;

import com.mp3player.music.domain.port.AlbumCoverSearcher;
import com.mp3player.music.infrastructure.CoverProperties;
import com.mp3player.music.infrastructure.CoverDownloader;
import com.mp3player.music.infrastructure.DeezerCoverSearcher;
import com.mp3player.music.infrastructure.CompositeCoverSearcher;
import com.mp3player.music.infrastructure.ItunesCoverSearcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuração do bean responsável pela busca de capas de álbuns.
 */
@Configuration
public class CoverSearcherConfig {

    /**
     * Cria o bean {@link AlbumCoverSearcher} composto, que busca capas
     * em múltiplas fontes (iTunes e Deezer).
     *
     * @param downloader utilitário de download de imagens
     * @param props      propriedades de configuração de capas
     * @return instância de {@link AlbumCoverSearcher} composta
     */
    @Bean
    public AlbumCoverSearcher albumCoverSearcher(CoverDownloader downloader, CoverProperties props) {
        var itunes = new ItunesCoverSearcher(downloader, props);
        var deezer = new DeezerCoverSearcher(downloader, props);
        return new CompositeCoverSearcher(List.of(itunes, deezer));
    }
}
