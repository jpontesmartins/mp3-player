package com.mp3player.lyrics.infrastructure;

import com.mp3player.lyrics.domain.port.LyricsScraper;
import com.mp3player.lyrics.domain.port.LyricsSource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * Orquestrador que tenta múltiplas {@link LyricsSource}s em ordem de prioridade.
 * Implementa {@link LyricsScraper} para integração transparente com o resto da aplicação.
 */
@Component
public class CompositeLyricsScraper implements LyricsScraper {

    private static final Logger log = LoggerFactory.getLogger(CompositeLyricsScraper.class);

    private final List<LyricsSource> sources;

    public CompositeLyricsScraper(List<LyricsSource> sources) {
        this.sources = sources.stream()
                .filter(LyricsSource::isEnabled)
                .sorted(Comparator.comparingInt(LyricsSource::getPriority))
                .toList();

        log.info("[Scraper] Fontes habilitadas: {}",
                this.sources.stream().map(LyricsSource::getName).toList());
    }

    @Override
    public String fetch(String artist, String title) throws IOException {
        for (LyricsSource source : sources) {
            String result = trySource(source, artist, title);
            if (result != null) return result;
        }

        log.warn("[Scraper] Nenhuma fonte encontrou letra para \"{}\" - \"{}\"", artist, title);
        return "Letra não encontrada para \"" + title + "\" de " + artist;
    }

    private String trySource(LyricsSource source, String artist, String title) {
        try {
            log.info("[Scraper] Tentando fonte: {}", source.getName());

            String pageUrl = source.findPage(artist, title);
            if (pageUrl == null) {
                log.info("[{}] Nenhuma URL encontrada", source.getName());
                return null;
            }

            // Remove sufixo de tradução se presente
            pageUrl = stripTranslation(pageUrl);

            log.info("[{}] Buscando página de letra: {}", source.getName(), pageUrl);
            Document doc = Jsoup.connect(pageUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .get();

            String lyrics = source.extractLyrics(doc);
            if (lyrics == null) {
                log.warn("[{}] Letra não encontrada na página {}", source.getName(), pageUrl);
                return null;
            }

            return lyrics;
        } catch (IOException e) {
            log.warn("[{}] Falha ao buscar letra: {}", source.getName(), e.getMessage());
            return null;
        }
    }

    private static String stripTranslation(String href) {
        if (href != null && href.endsWith("traducao.html")) {
            return href.substring(0, href.length() - "traducao.html".length());
        }
        return href;
    }
}
