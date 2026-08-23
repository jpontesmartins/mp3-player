package com.mp3player.lyrics.web;

import com.mp3player.lyrics.application.LyricsService;
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
 * {@link LyricsService}.
 */
@RestController
public class LyricsController {

    private final LyricsService lyricsService;

    /**
     * Cria uma nova instância do controller de letras.
     *
     * @param lyricsService serviço de letras
     */
    public LyricsController(LyricsService lyricsService) {
        this.lyricsService = lyricsService;
    }

    /**
     * Consulta a letra em cache para a música informada.
     *
     * @param path caminho absoluto do arquivo de áudio
     * @return {@link ResponseEntity} com a letra ou 404 se não encontrada
     */
    @GetMapping("/lyrics/cached")
    public ResponseEntity<String> getCachedLyrics(@RequestParam String path) {
        String lyrics = lyricsService.getCached(path);
        if (lyrics == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(lyrics);
    }

    /**
     * Retorna a letra da música, buscando em cache ou na web.
     *
     * @param path caminho absoluto do arquivo de áudio
     * @return {@link ResponseEntity} com a letra ou erro
     */
    @GetMapping("/lyrics")
    public ResponseEntity<String> getLyrics(@RequestParam String path) {
        try {
            return ResponseEntity.ok(lyricsService.get(path));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar letra: " + e.getMessage());
        }
    }

    /**
     * Requisição para salvar uma letra.
     *
     * @param path caminho absoluto do arquivo de áudio
     * @param text texto da letra
     */
    public record LyricsSaveRequest(String path, String text) {}

    /**
     * Salva a letra informada para a música.
     *
     * @param request requisição com caminho e texto da letra
     * @return {@link ResponseEntity} com mensagem de sucesso ou erro
     */
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

    /**
     * Remove a letra em cache para a música informada.
     *
     * @param path caminho absoluto do arquivo de áudio
     * @return {@link ResponseEntity} com mensagem de sucesso ou erro
     */
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