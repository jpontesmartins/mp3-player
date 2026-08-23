package com.mp3player.lyrics.domain.port;

import java.io.IOException;

/**
 * Port para buscar o texto da letra em uma fonte web.
 */
public interface LyricsScraper {

    /**
     * Busca a letra para o artista/título informados.
     *
     * @param artist nome do artista
     * @param title título da música
     * @return o texto da letra ou uma mensagem legível de "não encontrado"
     * @throws IOException se ocorrer erro de rede ao acessar a fonte
     */
    String fetch(String artist, String title) throws IOException;
}