package com.mp3player.config;

import com.mp3player.domain.port.AlbumCoverSearcher;
import com.mp3player.infrastructure.cover.CoverDownloader;
import com.mp3player.infrastructure.cover.DeezerCoverSearcher;
import com.mp3player.infrastructure.cover.CompositeCoverSearcher;
import com.mp3player.infrastructure.cover.ItunesCoverSearcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CoverSearcherConfig {

    @Bean
    public AlbumCoverSearcher albumCoverSearcher(CoverDownloader downloader, CoverProperties props) {
        var itunes = new ItunesCoverSearcher(downloader, props);
        var deezer = new DeezerCoverSearcher(downloader, props);
        return new CompositeCoverSearcher(List.of(itunes, deezer));
    }
}
