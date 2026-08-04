package com.mp3player.controller;

import com.mp3player.application.lyrics.LyricsAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador HTTP para o módulo de letras: consulta em cache e busca na web com
 * armazenamento em cache. Apenas traduz requisições web em chamadas ao
 * {@link LyricsAppService}.
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

    public record LyricsSaveRequest(String path, String text) {}

    @PostMapping("/lyrics")
    public ResponseEntity<String> saveLyrics(@RequestBody LyricsSaveRequest request) {
        if (request.path() == null || request.text() == null) {
            return ResponseEntity.badRequest().body("Caminho e texto são obrigatórios");
        }
        try {
            lyricsService.save(request.path(), request.text());
            return ResponseEntity.ok("Letra salva");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao salvar letra: " + e.getMessage());
        }
    }

    @DeleteMapping("/lyrics")
    public ResponseEntity<String> deleteLyrics(@RequestParam String path) {
        if (path == null || path.isBlank()) {
            return ResponseEntity.badRequest().body("Caminho é obrigatório");
        }
        try {
            lyricsService.delete(path);
            return ResponseEntity.ok("Letra removida");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao remover letra: " + e.getMessage());
        }
    }
}