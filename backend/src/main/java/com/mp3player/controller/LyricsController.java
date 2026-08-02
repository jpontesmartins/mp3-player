package com.mp3player.controller;

import com.mp3player.application.lyrics.LyricsAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP adapter for the lyrics module: cached lookup and web-fetch with cache.
 * Only translates web requests into {@link LyricsAppService} calls.
 */
@RestController
public class LyricsController {

    private final LyricsAppService lyricsService;

    public LyricsController(LyricsAppService lyricsService) {
        this.lyricsService = lyricsService;
    }

    @GetMapping("/lyrics/cached")
    public ResponseEntity<String> getCachedLyrics(@RequestParam String path) {
        String lyrics = lyricsService.getCached(path);
        if (lyrics == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(lyrics);
    }

    @GetMapping("/lyrics")
    public ResponseEntity<String> getLyrics(@RequestParam String path) {
        try {
            return ResponseEntity.ok(lyricsService.get(path));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar letra: " + e.getMessage());
        }
    }
}