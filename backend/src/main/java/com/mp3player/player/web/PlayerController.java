package com.mp3player.player.web;

import com.mp3player.player.application.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Adaptador HTTP para o controle de reprodução. Apenas traduz requisições web
 * em chamadas ao {@link PlayerService}; toda a lógica de negócio vive na camada
 * de aplicação.
 */
@RestController
@RequestMapping
public class PlayerController {

    private static final Logger log = LoggerFactory.getLogger(PlayerController.class);

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/play")
    public ResponseEntity<String> play(@RequestBody String filePath) {
        log.info("[Player] Reproduzindo: {}", filePath);
        try {
            playerService.play(filePath);
            return ResponseEntity.ok("Playing: " + filePath);
        } catch (Exception e) {
            log.error("[Player] Falha ao reproduzir: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/pause")
    public ResponseEntity<String> pause() {
        String result = playerService.pause();
        if ("Paused".equals(result)) {
            log.info("[Player] Pausado");
            return ResponseEntity.ok(result);
        }
        log.warn("[Player] Pausa ignorada: {}", result);
        return ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stop() {
        playerService.stop();
        log.info("[Player] Parado");
        return ResponseEntity.ok("Stopped");
    }

    @PostMapping("/seek")
    public ResponseEntity<String> seek(@RequestBody Map<String, Long> body) {
        Long position = body.get("position");
        if (position == null) {
            return ResponseEntity.badRequest().body("Missing position");
        }
        String result = playerService.seekTo(position);
        if (result.startsWith("Seeked")) {
            log.info("[Player] Buscando posição {}ms", position);
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/resume")
    public ResponseEntity<String> resume() {
        String result = playerService.resume();
        if ("Resumed".equals(result)) {
            log.info("[Player] Retomado");
            return ResponseEntity.ok(result);
        }
        log.warn("[Player] Retomada ignorada: {}", result);
        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/playing")
    public ResponseEntity<Map<String, Object>> playing() {
        return ResponseEntity.ok(playerService.status());
    }
}