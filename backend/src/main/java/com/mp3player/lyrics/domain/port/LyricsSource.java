package com.mp3player.lyrics.domain.port;

import org.jsoup.nodes.Document;

import java.io.IOException;

/**
 * Strategy para uma fonte de letras. Cada implementação sabe buscar
 * a URL da letra e extrair o texto de um {@link Document}.
 */
public interface LyricsSource {

    /** Nome legível da fonte (ex.: "letras.mus.br"). */
    String getName();

    /** Prioridade (menor = mais prioritário). */
    int getPriority();

    /** Habilitada via configuração. */
    boolean isEnabled();

    /**
     * Busca a URL da página da letra para o artista/título informados.
     *
     * @return URL da página ou {@code null} se não encontrar.
     */
    String findPage(String artist, String title) throws IOException;

    /**
     * Extrai o texto da letra de uma página já carregada.
     *
     * @param page documento HTML da página da letra
     * @return texto da letra ou {@code null} se não encontrou o elemento
     */
    String extractLyrics(Document page);
}
