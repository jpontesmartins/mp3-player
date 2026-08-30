package com.ovelha.fy.lyrics.domain.port;

import org.jsoup.nodes.Document;

import java.io.IOException;

/**
 * Strategy para uma fonte de letras. Cada implementação sabe buscar
 * a URL da letra e extrair o texto de um {@link Document}.
 */
public interface LyricsSource {

    /**
     * Nome legível da fonte (ex.: "letras.mus.br").
     *
     * @return nome descritivo da fonte
     */
    String getName();

    /**
     * Prioridade da fonte (menor valor = mais prioritário).
     *
     * @return número inteiro representando a prioridade
     */
    int getPriority();

    /**
     * Indica se a fonte está habilitada via configuração.
     *
     * @return {@code true} se habilitada, {@code false} caso contrário
     */
    boolean isEnabled();

    /**
     * Busca a URL da página da letra para o artista/título informados.
     *
     * @param artist nome do artista
     * @param title título da música
     * @return URL da página ou {@code null} se não encontrar
     * @throws IOException se ocorrer erro de rede ao buscar a URL
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
