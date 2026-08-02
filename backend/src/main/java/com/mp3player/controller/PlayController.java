package com.mp3player.controller;

import com.mp3player.application.lyrics.LyricsAppService;
import com.mp3player.application.metadata.Id3AppService;
import com.mp3player.application.player.PlayerService;
import com.mp3player.application.playlist.PlaylistAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * HTTP adapter. Only translates web requests into application service calls and
 * formats responses; all business logic lives in the application layer.
 */
@RestController
public class PlayController {

    private static final Logger log = LoggerFactory.getLogger(PlayController.class);

    private final PlayerService playerService;
    private final PlaylistAppService playlistService;
    private final LyricsAppService lyricsService;
    private final Id3AppService id3Service;

    public PlayController(PlayerService playerService, PlaylistAppService playlistService,
                          LyricsAppService lyricsService, Id3AppService id3Service) {
        this.playerService = playerService;
        this.playlistService = playlistService;
        this.lyricsService = lyricsService;
        this.id3Service = id3Service;
    }

    // ---------- player ----------

    @PostMapping("/play")
    public ResponseEntity<String> play(@RequestBody String filePath) {
        log.info("▶ Play: {}", filePath);
        try {
            playerService.play(filePath);
            return ResponseEntity.ok("Playing: " + filePath);
        } catch (Exception e) {
            log.error("▶ Play failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/pause")
    public ResponseEntity<String> pause() {
        String result = playerService.pause();
        if ("Paused".equals(result)) {
            log.info("⏸ Paused");
            return ResponseEntity.ok(result);
        }
        log.warn("⏸ Pause ignored: {}", result);
        return ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stop() {
        playerService.stop();
        log.info("⏹ Stopped");
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
            log.info("⏩ Seek to {}ms", position);
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/resume")
    public ResponseEntity<String> resume() {
        String result = playerService.resume();
        if ("Resumed".equals(result)) {
            log.info("▶ Resumed");
            return ResponseEntity.ok(result);
        }
        log.warn("▶ Resume ignored: {}", result);
        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/playing")
    public ResponseEntity<Map<String, Object>> playing() {
        return ResponseEntity.ok(playerService.status());
    }

    // ---------- playlist ----------

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

    public record PlaylistRenameRequest(String oldName, String newName) {}

    @PostMapping("/playlist/rename")
    public ResponseEntity<?> renameVirtualPlaylist(@RequestBody PlaylistRenameRequest request) {
        try {
            playlistService.rename(request.oldName(), request.newName());
            return ResponseEntity.ok("Renamed");
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ---------- ID3 ----------

    public record Id3UpdateRequest(String path, Map<String, String> tags) {}

    @GetMapping("/id3")
    public ResponseEntity<Map<String, String>> getId3(@RequestParam String path) {
        return ResponseEntity.ok(id3Service.getForFile(path));
    }

    @PostMapping("/id3/update")
    public ResponseEntity<?> updateId3(@RequestBody Id3UpdateRequest request) {
        try {
            return ResponseEntity.ok(id3Service.update(request.path(), request.tags()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/id3/bulk")
    public ResponseEntity<Map<String, Map<String, String>>> getBulkId3(@RequestBody List<String> paths) {
        return ResponseEntity.ok(id3Service.getBulk(paths));
    }

    // ---------- lyrics ----------

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

    // ---------- cover file serving ----------

    @GetMapping("/cover")
    public ResponseEntity<Resource> getCover(@RequestParam String path) {
        try {
            Path parent = Paths.get(path).getParent();
            if (parent == null) return ResponseEntity.notFound().build();
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
                            if (!Arrays.asList(extensions).contains(ext)) return false;
                            return Arrays.asList(coverNames).contains(stem);
                        })
                        .findFirst();
                if (coverFile.isPresent()) {
                    var file = coverFile.get();
                    var resource = new InputStreamResource(java.nio.file.Files.newInputStream(file));
                    String name = file.getFileName().toString().toLowerCase();
                    String contentType = name.endsWith("png") ? "image/png" : "image/jpeg";
                    return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
                }
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}