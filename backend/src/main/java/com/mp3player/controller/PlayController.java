package com.mp3player.controller;

import com.mp3player.service.Mp3PlayService;
import com.mp3player.service.PlaylistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class PlayController {

    private final Mp3PlayService mp3PlayService;
    private final PlaylistService playlistService;

    public PlayController(Mp3PlayService mp3PlayService, PlaylistService playlistService) {
        this.mp3PlayService = mp3PlayService;
        this.playlistService = playlistService;
    }

    @PostMapping("/play")
    public ResponseEntity<String> play(@RequestBody String filePath) {
        try {
            mp3PlayService.play(filePath);
            return ResponseEntity.ok("Playing: " + filePath);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/pause")
    public ResponseEntity<String> pause() {
        if (!mp3PlayService.isPlaying()) {
            return ResponseEntity.badRequest().body("No music playing");
        }
        mp3PlayService.pause();
        return ResponseEntity.ok("Paused");
    }

    @PostMapping("/resume")
    public ResponseEntity<String> resume() {
        if (!mp3PlayService.isPlaying()) {
            return ResponseEntity.badRequest().body("No music playing");
        }
        if (!mp3PlayService.isPaused()) {
            return ResponseEntity.badRequest().body("Music is not paused");
        }
        mp3PlayService.resume();
        return ResponseEntity.ok("Resumed");
    }

    @GetMapping("/playlist")
    public ResponseEntity<?> getPlaylist(@RequestParam String path) {
        try {
            List<String> files = playlistService.scanFolder(path);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/id3")
    public ResponseEntity<Map<String, String>> getId3(@RequestParam String path) {
        return ResponseEntity.ok(mp3PlayService.getId3TagsForFile(path));
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
