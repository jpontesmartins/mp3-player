package com.mp3player.metadata.infrastructure;

import com.mp3player.metadata.domain.model.CoverImage;
import com.mp3player.metadata.domain.port.AlbumCoverSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class CompositeCoverSearcher implements AlbumCoverSearcher {

    private static final Logger log = LoggerFactory.getLogger(CompositeCoverSearcher.class);

    private final List<AlbumCoverSearcher> searchers;

    public CompositeCoverSearcher(List<AlbumCoverSearcher> searchers) {
        this.searchers = searchers;
    }

    @Override
    public CoverImage findCover(String query) throws IOException {
        for (AlbumCoverSearcher searcher : searchers) {
            CoverImage cover = searcher.findCover(query);
            if (cover != null && !cover.isEmpty()) {
                return cover;
            }
            log.info("[Capa] {} não encontrou capa para \"{}\", tentando próximo", searcher.getClass().getSimpleName(), query);
        }
        log.warn("[Capa] Nenhuma capa encontrada para: {}", query);
        return null;
    }
}
