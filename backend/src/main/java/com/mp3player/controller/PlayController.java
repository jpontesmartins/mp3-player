package com.mp3player.controller;

import com.mp3player.service.LyricsService;
import com.mp3player.service.Mp3PlayService;
import com.mp3player.service.PlaylistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

@RestController
public class PlayController {

    private static final Logger log = LoggerFactory.getLogger(PlayController.class);

    private final Mp3PlayService mp3PlayService;
    private final PlaylistService playlistService;
    private final LyricsService lyricsService;

    public PlayController(Mp3PlayService mp3PlayService, PlaylistService playlistService, LyricsService lyricsService) {
        this.mp3PlayService = mp3PlayService;
        this.playlistService = playlistService;
        this.lyricsService = lyricsService;
    }

    @PostMapping("/play")
    public ResponseEntity<String> play(@RequestBody String filePath) {
        log.info("▶ Play: {}", filePath);
        try {
            mp3PlayService.play(filePath);
            return ResponseEntity.ok("Playing: " + filePath);
        } catch (Exception e) {
            log.error("▶ Play failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/pause")
    public ResponseEntity<String> pause() {
        if (!mp3PlayService.isPlaying()) {
            log.warn("⏸ Pause ignored — no music playing");
            return ResponseEntity.badRequest().body("No music playing");
        }
        mp3PlayService.pause();
        log.info("⏸ Paused");
        return ResponseEntity.ok("Paused");
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stop() {
        mp3PlayService.stop();
        log.info("⏹ Stopped");
        return ResponseEntity.ok("Stopped");
    }

    @PostMapping("/seek")
    public ResponseEntity<String> seek(@RequestBody Map<String, Long> body) {
        Long position = body.get("position");
        if (position == null) {
            log.warn("⏩ Seek ignored — missing position");
            return ResponseEntity.badRequest().body("Missing position");
        }
        mp3PlayService.seekTo(position);
        log.info("⏩ Seek to {}ms", position);
        return ResponseEntity.ok("Seeked to " + position);
    }

    @PostMapping("/resume")
    public ResponseEntity<String> resume() {
        if (!mp3PlayService.isPlaying()) {
            log.warn("▶ Resume ignored — no music playing");
            return ResponseEntity.badRequest().body("No music playing");
        }
        if (!mp3PlayService.isPaused()) {
            log.warn("▶ Resume ignored — music is not paused");
            return ResponseEntity.badRequest().body("Music is not paused");
        }
        mp3PlayService.resume();
        log.info("▶ Resumed");
        return ResponseEntity.ok("Resumed");
    }

    @GetMapping("/playlist")
    public ResponseEntity<?> getPlaylist(@RequestParam String path) {
        log.info("📂 Playlist: {}", path);
        try {
            List<String> files = playlistService.scanFolder(path);
            log.info("📂 Found {} files", files.size());
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            log.error("📂 Playlist failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/cover")
    public ResponseEntity<Resource> getCover(@RequestParam String path) {
        java.nio.file.Path parent = java.nio.file.Paths.get(path).getParent();
        if (parent == null) {
            log.warn("🖼 Cover: no parent directory for {}", path);
            return ResponseEntity.notFound().build();
        }
        String[] coverNames = { "cover", "folder", "album", "front", "art", "artwork" };
        String[] extensions = { "jpg", "jpeg", "png" };
        try (var files = java.nio.file.Files.list(parent)) {
            var coverFile = files
                .filter(f -> {
                    if (!java.nio.file.Files.isRegularFile(f)) return false;
                    String name = f.getFileName().toString().toLowerCase();
                    int dot = name.lastIndexOf('.');
                    if (dot < 0) return false;
                    String stem = name.substring(0, dot);
                    String ext = name.substring(dot + 1);
                    if (!java.util.Arrays.asList(extensions).contains(ext)) return false;
                    return java.util.Arrays.asList(coverNames).contains(stem);
                })
                .findFirst();
            if (coverFile.isPresent()) {
                var file = coverFile.get();
                log.info("🖼 Cover found: {}", file.getFileName());
                var resource = new InputStreamResource(java.nio.file.Files.newInputStream(file));
                String name = file.getFileName().toString().toLowerCase();
                String contentType = name.endsWith("png") ? "image/png" : "image/jpeg";
                return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
            }
            log.info("🖼 Cover not found in {}", parent);
        } catch (Exception e) {
            log.error("🖼 Cover error: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/lyrics/cached")
    public ResponseEntity<String> getCachedLyrics(@RequestParam String path) {
        try {
            String lyrics = lyricsService.getCachedLyrics(path);
            if (lyrics != null) {
                log.info("📜 Cached lyrics found");
                return ResponseEntity.ok(lyrics);
            }
            log.info("📜 No cached lyrics");
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("📜 Cached lyrics error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    @GetMapping("/lyrics")
    public ResponseEntity<String> getLyrics(@RequestParam String path) {
        log.info("📜 Fetching lyrics");
        try {
            String lyrics = lyricsService.getLyrics(path);
            return ResponseEntity.ok(lyrics);
        } catch (Exception e) {
            log.error("📜 Lyrics fetch failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Erro ao buscar letra: " + e.getMessage());
        }
    }

    @GetMapping("/id3")
    public ResponseEntity<Map<String, String>> getId3(@RequestParam String path) {
        log.info("🏷 ID3: {}", path);
        return ResponseEntity.ok(mp3PlayService.getId3TagsForFile(path));
    }

    @PostMapping("/id3/bulk")
    public ResponseEntity<Map<String, Map<String, String>>> getBulkId3(@RequestBody List<String> paths) {
        log.info("🏷 ID3 bulk: {} files", paths.size());
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (String path : paths) {
            result.put(path, mp3PlayService.getId3TagsForFile(path));
        }
        log.info("🏷 ID3 bulk done: {} files", result.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/playing")
    public ResponseEntity<Map<String, Object>> getPlaying() {
        String filePath = mp3PlayService.getCurrentFilePath();
        if (filePath == null || !mp3PlayService.isPlaying()) {
            return ResponseEntity.ok(Map.of("status", "stopped", "file", ""));
        }
        String status = mp3PlayService.isPaused() ? "paused" : "playing";
        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("file", filePath);
        response.put("position", mp3PlayService.getElapsedMillis());
        response.put("duration", mp3PlayService.getTotalMillis());
        response.put("id3", mp3PlayService.getId3Tags());
        return ResponseEntity.ok(response);
    }
}
