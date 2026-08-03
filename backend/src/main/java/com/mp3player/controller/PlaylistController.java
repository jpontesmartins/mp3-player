package com.mp3player.controller;

import com.mp3player.application.playlist.PlaylistAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador HTTP para o módulo de playlist: scan de pasta e CRUD de playlists
 * virtuais. Apenas traduz requisições web em chamadas ao {@link PlaylistAppService}.
 */
@RestController
public class PlaylistController {

    private static final Logger log = LoggerFactory.getLogger(PlaylistController.class);

    private final PlaylistAppService playlistService;

    public PlaylistController(PlaylistAppService playlistService) {
        this.playlistService = playlistService;
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

    @GetMapping("/playlists")
    public ResponseEntity<?> listPlaylists() {
        try {
            return ResponseEntity.ok(playlistService.list());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/playlist/{name}")
    public ResponseEntity<?> getVirtualPlaylist(@PathVariable String name) {
        try {
            return ResponseEntity.ok(playlistService.load(name));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    public record PlaylistSaveRequest(String name, List<String> paths) {}

    @PostMapping("/playlist")
    public ResponseEntity<?> saveVirtualPlaylist(@RequestBody PlaylistSaveRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().body("Missing playlist name");
        }
        try {
            playlistService.createOrUpdate(request.name(), request.paths() == null ? List.of() : request.paths());
            return ResponseEntity.ok("Saved");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/playlist/{name}")
    public ResponseEntity<?> deleteVirtualPlaylist(@PathVariable String name) {
        try {
            playlistService.delete(name);
            return ResponseEntity.ok("Deleted");
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/playlist/rename")
    public ResponseEntity<?> renameVirtualPlaylist(@RequestBody PlaylistRenameRequest request) {
        try {
            playlistService.rename(request.oldName(), request.newName());
            return ResponseEntity.ok("Renamed");
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    public record PlaylistRenameRequest(String oldName, String newName) {}
}