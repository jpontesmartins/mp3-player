package com.ovelha.fy.lyrics.infrastructure;

import com.ovelha.fy.lyrics.domain.port.LyricsScraper;
import com.ovelha.fy.lyrics.domain.port.LyricsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    /** Orçamento de tempo padrão (ms) por fonte, ao tentar todos os artistas candidatos. */
    static final long DEFAULT_MAX_MILLIS = 30_000L;

    private final List<LyricsSource> sources;
    private final long maxMillis;

    /**
     * Cria um scraper composto com as fontes habilitadas, ordenadas por prioridade.
     *
     * @param sources lista de todas as fontes de letras disponíveis
     */
    public CompositeLyricsScraper(List<LyricsSource> sources) {
        this(sources, DEFAULT_MAX_MILLIS);
    }

    /**
     * Cria um scraper composto com orçamento de tempo explícito por fonte.
     *
     * @param sources lista de todas as fontes de letras disponíveis
     * @param maxMillis tempo máximo (ms) em uma fonte antes de passar para a próxima
     *                  ({@code mp3.lyrics.source-budget-ms})
     */
    @Autowired
    public CompositeLyricsScraper(List<LyricsSource> sources,
                                  @Value("${mp3.lyrics.source-budget-ms:30000}") long maxMillis) {
        this.maxMillis = maxMillis;
        this.sources = sources.stream()
                .filter(LyricsSource::isEnabled)
                .sorted(Comparator.comparingInt(LyricsSource::getPriority))
                .toList();

        log.info("[Scraper] Fontes habilitadas: {}",
                this.sources.stream().map(LyricsSource::getName).toList());
    }

    /**
     * Tenta buscar a letra em cada fonte habilitada (por prioridade) e, em cada fonte,
     * em cada artista candidato. Cada fonte tem seu próprio orçamento de tempo.
     *
     * @param artistCandidates nomes de artista a tentar
     * @param title título da música
     * @return texto da letra encontrada, ou vazio se nenhuma fonte a possui
     */
    @Override
    public Optional<String> fetch(List<String> artistCandidates, String title) {
        log.info("[Scraper] Candidatos de artista: {}", artistCandidates);
        for (LyricsSource source : sources) {
            long deadline = System.nanoTime() + maxMillis * 1_000_000L;
            for (String artist : artistCandidates) {
                if (System.nanoTime() > deadline) {
                    log.warn("[{}] Orçamento de {} ms esgotado para \"{}\"; passando para a próxima fonte",
                            source.getName(), maxMillis, title);
                    break;
                }
                String result = trySource(source, artist, title);
                if (result != null) return Optional.of(result);
            }
        }

        log.warn("[Scraper] Nenhuma fonte encontrou letra para {} - \"{}\"", artistCandidates, title);
        return Optional.empty();
    }

    /**
     * Tenta buscar a letra em uma fonte específica, delegando o HTTP à própria fonte.
     *
     * @param source fonte de letras a ser consultada
     * @param artist nome do artista
     * @param title título da música
     * @return texto da letra encontrada, ou {@code null} se a fonte não possui a letra
     */
    private String trySource(LyricsSource source, String artist, String title) {
        try {
            log.info("[Scraper] Tentando fonte: {} (artista: {})", source.getName(), artist);
            String lyrics = source.fetchFromSource(artist, title);
            if (lyrics == null) {
                log.info("[{}] Letra não encontrada para o artista {}", source.getName(), artist);
            }
            return lyrics;
        } catch (IOException | RuntimeException e) {
            log.warn("[{}] Falha ao buscar letra: {}", source.getName(), e.toString());
            return null;
        }
    }
}
