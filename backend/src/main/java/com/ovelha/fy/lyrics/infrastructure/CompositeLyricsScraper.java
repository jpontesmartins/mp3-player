package com.ovelha.fy.lyrics.infrastructure;

import com.ovelha.fy.lyrics.domain.port.LyricsScraper;
import com.ovelha.fy.lyrics.domain.port.LyricsSource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Orquestrador que tenta múltiplas {@link LyricsSource}s em ordem de prioridade.
 * Implementa {@link LyricsScraper} para integração transparente com o resto da aplicação.
 */
@Component
public class CompositeLyricsScraper implements LyricsScraper {

    private static final Logger log = LoggerFactory.getLogger(CompositeLyricsScraper.class);

    private final List<LyricsSource> sources;

    /**
     * Cria um scraper composto com as fontes habilitadas, ordenadas por prioridade.
     *
     * @param sources lista de todas as fontes de letras disponíveis
     */
    public CompositeLyricsScraper(List<LyricsSource> sources) {
        this.sources = sources.stream()
                .filter(LyricsSource::isEnabled)
                .sorted(Comparator.comparingInt(LyricsSource::getPriority))
                .toList();

        log.info("[Scraper] Fontes habilitadas: {}",
                this.sources.stream().map(LyricsSource::getName).toList());
    }

    /**
     * Tenta buscar a letra em cada fonte habilitada (por prioridade) e, em cada fonte,
     * em cada artista candidato.
     *
     * @param artistCandidates nomes de artista a tentar
     * @param title título da música
     * @return texto da letra encontrada, ou vazio se nenhuma fonte a possui
     */
    @Override
    public Optional<String> fetch(List<String> artistCandidates, String title) {
        log.info("[Scraper] Candidatos de artista: {}", artistCandidates);
        for (LyricsSource source : sources) {
            for (String artist : artistCandidates) {
                String result = trySource(source, artist, title);
                if (result != null) return Optional.of(result);
            }
        }

        log.warn("[Scraper] Nenhuma fonte encontrou letra para {} - \"{}\"", artistCandidates, title);
        return Optional.empty();
    }

    /**
     * Tenta buscar a letra em uma fonte específica.
     *
     * @param source fonte de letras a ser consultada
     * @param artist nome do artista
     * @param title título da música
     * @return texto da letra encontrada, ou {@code null} se a fonte não possui a letra
     */
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
                    .userAgent(source.getUserAgent())
                    .referrer("https://www.letras.mus.br")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .header("Cache-Control", "max-age=0")
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

    /**
     * Remove o sufixo de tradução de uma URL, se presente.
     *
     * @param href URL original
     * @return URL sem o sufixo "traducao.html"
     */
    private static String stripTranslation(String href) {
        if (href != null && href.endsWith("traducao.html")) {
            return href.substring(0, href.length() - "traducao.html".length());
        }
        return href;
    }
}
