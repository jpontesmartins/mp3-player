package com.ovelha.fy.shared.config;

import com.ovelha.fy.player.music.domain.port.AlbumCoverSearcher;
import com.ovelha.fy.player.music.infrastructure.CoverProperties;
import com.ovelha.fy.player.music.infrastructure.CoverDownloader;
import com.ovelha.fy.player.music.infrastructure.DeezerCoverSearcher;
import com.ovelha.fy.player.music.infrastructure.CompositeCoverSearcher;
import com.ovelha.fy.player.music.infrastructure.ItunesCoverSearcher;
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
