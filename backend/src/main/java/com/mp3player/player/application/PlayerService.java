package com.mp3player.player.application;

import com.mp3player.player.domain.port.PlayerEngine;
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

    /**
     * Construtor com injeção de dependência do motor de reprodução.
     *
     * @param engine implementação do port {@link PlayerEngine}
     */
    public PlayerService(PlayerEngine engine) {
        this.engine = engine;
    }

    /**
     * Inicia a reprodução do arquivo informado.
     *
     * @param filePath caminho absoluto do arquivo MP3
     * @throws IOException se ocorrer erro ao acessar o arquivo
     */
    public void play(String filePath) throws IOException {
        engine.play(filePath);
    }

    /**
     * Pausa a reprodução atual.
     *
     * @return mensagem indicando o resultado da operação
     */
    public String pause() {
        if (!engine.isPlaying()) return "No music playing";
        engine.pause();
        return "Paused";
    }

    /**
     * Retoma a música pausada.
     *
     * @return mensagem indicando o resultado da operação
     */
    public String resume() {
        if (!engine.isPlaying()) return "No music playing";
        if (!engine.isPaused()) return "Music is not paused";
        engine.resume();
        return "Resumed";
    }

    /** Para a reprodução e limpa o estado interno. */
    public void stop() {
        engine.stop();
    }

    /**
     * Salta para a posição informada em milissegundos.
     *
     * @param positionMillis posição desejada em milissegundos
     * @return mensagem indicando o resultado da operação
     */
    public String seekTo(long positionMillis) {
        if (engine.getCurrentFilePath() == null) return "No music playing";
        engine.seekTo(positionMillis);
        return "Seeked to " + positionMillis;
    }

    /**
     * Retorna o estado atual da reprodução (status, arquivo, posição, duração e ID3).
     *
     * @return mapa com os campos do estado da reprodução
     */
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