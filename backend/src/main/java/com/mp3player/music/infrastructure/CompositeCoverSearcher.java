package com.mp3player.music.infrastructure;

import com.mp3player.music.domain.model.CoverImage;
import com.mp3player.music.domain.port.AlbumCoverSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Buscador de capas que tenta múltiplos {@link AlbumCoverSearcher} em sequência,
 * retornando a primeira imagem encontrada.
 */
public class CompositeCoverSearcher implements AlbumCoverSearcher {

    private static final Logger log = LoggerFactory.getLogger(CompositeCoverSearcher.class);

    private final List<AlbumCoverSearcher> searchers;

    /**
     * Construtor do buscador composto.
     *
     * @param searchers lista de buscadores a serem tentados em sequência
     */
    public CompositeCoverSearcher(List<AlbumCoverSearcher> searchers) {
        this.searchers = searchers;
    }

    /**
     * Busca a primeira imagem de capa encontrada entre os buscadores registrados.
     *
     * @param query termo de busca (ex: "artista álbum")
     * @return imagem de capa encontrada, ou {@code null} se nenhum buscador encontrou
     * @throws IOException se ocorrer erro de rede ou leitura
     */
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
