package com.ovelha.fy.player.domain.port;

import java.io.IOException;
import java.util.Map;

/**
 * Interface dos controles do tocador.
 * Port (contrato) para o motor de reprodução de áudio. A implementação é
 * responsável pela decodificação e pelas threads de baixo nível (atualmente JLayer).
 */
public interface PlayerEngine {

    /**
     * Inicia a reprodução do arquivo a partir do início.
     *
     * @param filePath caminho absoluto do arquivo MP3
     * @throws IOException se ocorrer erro ao acessar o arquivo
     */
    void play(String filePath) throws IOException;

    /**
     * Inicia a reprodução do arquivo a partir de uma posição específica.
     *
     * @param filePath            caminho absoluto do arquivo MP3
     * @param startPositionMillis posição inicial em milissegundos
     * @throws IOException se ocorrer erro ao acessar o arquivo
     */
    void play(String filePath, long startPositionMillis) throws IOException;

    /** Pausa a reprodução atual. */
    void pause();

    /** Retoma a reprodução pausada. */
    void resume();

    /**
     * Salta para a posição especificada.
     *
     * @param positionMillis posição desejada em milissegundos
     */
    void seekTo(long positionMillis);

    /** Para a reprodução e libera recursos. */
    void stop();

    /**
     * Indica se há áudio sendo reproduzido (incluindo pausado).
     *
     * @return {@code true} se estiver reproduzindo ou pausado
     */
    boolean isPlaying();

    /**
     * Indica se a reprodução está pausada.
     *
     * @return {@code true} se estiver pausado
     */
    boolean isPaused();

    /**
     * Retorna o caminho do arquivo em reprodução.
     *
     * @return caminho do arquivo ou {@code null} se nenhum estiver tocando
     */
    String getCurrentFilePath();

    /**
     * Retorna o tempo decorrido desde o início da reprodução (descontando pausas).
     *
     * @return tempo em milissegundos
     */
    long getElapsedMillis();

    /**
     * Retorna a duração total do arquivo em reprodução.
     *
     * @return duração em milissegundos
     */
    long getTotalMillis();

    /**
     * Tags ID3 da faixa em reprodução, como mapa de troca (wire).
     *
     * @return mapa de tags ID3 ou {@code null} se nenhum áudio estiver carregado
     */
    Map<String, String> getId3Tags();
}