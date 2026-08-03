package com.mp3player.domain.port;

import java.io.IOException;
import java.util.Map;

/**
 * Port (contrato) para o motor de reprodução de áudio. A implementação é
 * responsável pela decodificação e pelas threads de baixo nível (atualmente JLayer).
 */
public interface PlayerEngine {

    void play(String filePath) throws IOException;

    void play(String filePath, long startPositionMillis) throws IOException;

    void pause();

    void resume();

    void seekTo(long positionMillis);

    void stop();

    boolean isPlaying();

    boolean isPaused();

    String getCurrentFilePath();

    long getElapsedMillis();

    long getTotalMillis();

    /** Tags ID3 da faixa em reprodução, como mapa de troca (wire). */
    Map<String, String> getId3Tags();
}