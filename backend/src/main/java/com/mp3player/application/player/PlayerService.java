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
 * Application service for the player module. Orchestrates the {@link PlayerEngine}
 * port and implements play/pause/stop/resume/seek and next/previous navigation.
 */
@Service
public class PlayerService {

    private final PlayerEngine engine;

    public PlayerService(PlayerEngine engine) {
        this.engine = engine;
    }

    public void play(String filePath) throws IOException {
        engine.play(filePath);
    }

    public void play(String filePath, long startMillis) throws IOException {
        engine.play(filePath, startMillis);
    }

    public String pause() {
        if (!engine.isPlaying()) return "No music playing";
        engine.pause();
        return "Paused";
    }

    public String resume() {
        if (!engine.isPlaying()) return "No music playing";
        if (!engine.isPaused()) return "Music is not paused";
        engine.resume();
        return "Resumed";
    }

    public void stop() {
        engine.stop();
    }

    public String seekTo(long positionMillis) {
        if (engine.getCurrentFilePath() == null) return "No music playing";
        engine.seekTo(positionMillis);
        return "Seeked to " + positionMillis;
    }

    /** Plays the next song in the list according to the given playback mode. */
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

    /** Plays the previous song in the list according to the given playback mode. */
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