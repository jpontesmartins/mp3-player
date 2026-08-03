package com.mp3player.domain.port;

import java.io.IOException;

/**
 * Port para buscar o texto da letra em uma fonte web.
 */
public interface LyricsScraper {

    /**
     * Busca a letra para o artista/título informados.
     *
     * @return o texto da letra ou uma mensagem legível de "não encontrado".
     */
    String fetch(String artist, String title) throws IOException;
}