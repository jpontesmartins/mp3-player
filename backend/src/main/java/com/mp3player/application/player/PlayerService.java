package com.mp3player.application.player;

import com.mp3player.domain.port.PlayerEngine;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service da aplicação para o módulo de player. Orquestra o port {@link PlayerEngine}
 * e implementa play/pause/stop/resume/seek e o relatório de status.
 */
@Service
public class PlayerService {

    private final PlayerEngine engine;

    public PlayerService(PlayerEngine engine) {
        this.engine = engine;
    }

    /** Inicia a reprodução do arquivo informado. */
    public void play(String filePath) throws IOException {
        engine.play(filePath);
    }

    /** Pausa a reprodução atual. */
    public String pause() {
        if (!engine.isPlaying()) return "No music playing";
        engine.pause();
        return "Paused";
    }

    /** Retoma a música pausada. */
    public String resume() {
        if (!engine.isPlaying()) return "No music playing";
        if (!engine.isPaused()) return "Music is not paused";
        engine.resume();
        return "Resumed";
    }

    /** Para a reprodução e limpa o estado. */
    public void stop() {
        engine.stop();
    }

    /** Salta para a posição informada em milissegundos. */
    public String seekTo(long positionMillis) {
        if (engine.getCurrentFilePath() == null) return "No music playing";
        engine.seekTo(positionMillis);
        return "Seeked to " + positionMillis;
    }

    /** Retorna o estado atual da reprodução (status, arquivo, posição, duração e ID3). */
    public Map<String, Object> status() {
        Map<String, Object> response = new LinkedHashMap<>();
        String filePath = engine.getCurrentFilePath();
        if (filePath == null || !engine.isPlaying()) {
            response.put("status", "stopped");
            response.put("file", "");
            return response;
        }
        response.put("status", engine.isPaused() ? "paused" : "playing");
        response.put("file", filePath);
        response.put("position", engine.getElapsedMillis());
        response.put("duration", engine.getTotalMillis());
        response.put("id3", engine.getId3Tags());
        return response;
    }
}