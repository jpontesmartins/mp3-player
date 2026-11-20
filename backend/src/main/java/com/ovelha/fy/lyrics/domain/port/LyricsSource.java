package com.ovelha.fy.lyrics.domain.port;

import java.io.IOException;

/**
 * Port para uma fonte de letras (site de letras). Não expõe detalhes de HTTP/HTML:
 * cada implementação sabe localizar e extrair a letra do seu site.
 */
public interface LyricsSource {

    /**
     * Nome legível da fonte (usado em logs).
     *
     * @return nome da fonte
     */
    String getName();

    /**
     * Prioridade da fonte (menor = consultada primeiro).
     *
     * @return prioridade
     */
    int getPriority();

    /**
     * Indica se a fonte está habilitada na configuração.
     *
     * @return {@code true} se habilitada
     */
    boolean isEnabled();

    /**
     * Fluxo completo: localiza a página da letra, carrega-a e extrai o texto.
     *
     * @param artist nome do artista
     * @param title título da música
     * @return texto da letra ou {@code null} se não encontrada
     * @throws IOException se ocorrer erro de rede
     */
    String fetchFromSource(String artist, String title) throws IOException;
}
