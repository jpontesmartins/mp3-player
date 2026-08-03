package com.mp3player.application.player;

import com.mp3player.domain.model.Settings;
import com.mp3player.domain.port.PlayerEngine;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service da aplicação para o módulo de player. Orquestra o port {@link PlayerEngine}
 * e implementa play/pause/stop/resume/seek e a navegação anterior/próxima.
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

    /** Inicia a reprodução do arquivo informado a partir da posição em milissegundos. */
    public void play(String filePath, long startMillis) throws IOException {
        engine.play(filePath, startMillis);
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

    // TODO acho que nao estah sendo usado
    /** Toca a próxima música da lista de acordo com o modo de reprodução. */
    public boolean playNext(List<String> files, Settings.PlaybackMode mode) {
        String next = next(engine.getCurrentFilePath(), files, mode);
        if (next == null) return false;
        try {
            play(next);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // TODO acho que nao estah sendo usado
    /** Toca a música anterior da lista de acordo com o modo de reprodução. */
    public boolean playPrevious(List<String> files, Settings.PlaybackMode mode) {
        String prev = previous(engine.getCurrentFilePath(), files, mode);
        if (prev == null) return false;
        try {
            play(prev);
            return true;
        } catch (IOException e) {
            return false;
        }
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

    String next(String current, List<String> files, Settings.PlaybackMode mode) {
        if (files == null || files.isEmpty()) return null;
        if (mode == Settings.PlaybackMode.REPEAT) return current;
        if (mode == Settings.PlaybackMode.SHUFFLE) return files.get(ThreadLocalRandom.current().nextInt(files.size()));
        if (current == null) return files.get(0);
        int idx = files.indexOf(current);
        if (idx < 0 || idx >= files.size() - 1) return files.get(0);
        return files.get(idx + 1);
    }

    String previous(String current, List<String> files, Settings.PlaybackMode mode) {
        if (files == null || files.isEmpty()) return null;
        if (mode == Settings.PlaybackMode.REPEAT) return current;
        if (mode == Settings.PlaybackMode.SHUFFLE) return files.get(ThreadLocalRandom.current().nextInt(files.size()));
        if (current == null) return files.get(files.size() - 1);
        int idx = files.indexOf(current);
        if (idx <= 0) return files.get(files.size() - 1);
        return files.get(idx - 1);
    }
}