package com.mp3player.playlist.web;

import com.mp3player.playlist.application.PlaylistService;
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
 * virtuais. Apenas traduz requisições web em chamadas ao {@link PlaylistService}.
 */
@RestController
public class PlaylistController {

    private static final Logger log = LoggerFactory.getLogger(PlaylistController.class);

    private final PlaylistService playlistService;

    /**
     * Construtor do controller de playlist.
     *
     * @param playlistService service de playlist da camada de aplicação.
     */
    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    /**
     * Escaneia uma pasta física e retorna os caminhos das músicas encontradas.
     *
     * @param path caminho absoluto da pasta a ser escaneada.
     * @return lista de caminhos das músicas ou erro em caso de falha.
     */
    @GetMapping("/playlist")
    public ResponseEntity<?> getPlaylist(@RequestParam String path) {
        log.info("[Playlist] Carregando pasta: {}", path);
        try {
            List<String> files = playlistService.scanFolder(path);
            log.info("[Playlist] {} arquivos encontrados", files.size());
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            log.error("[Playlist] Falha ao carregar pasta: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Lista todas as playlists salvas.
     *
     * @return lista de nomes de playlists ou erro em caso de falha.
     */
    @GetMapping("/playlists")
    public ResponseEntity<?> listPlaylists() {
        try {
            return ResponseEntity.ok(playlistService.list());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Carrega os caminhos das músicas de uma playlist virtual.
     *
     * @param name nome da playlist.
     * @return lista de caminhos das músicas ou 404 se não existir.
     */
    @GetMapping("/playlist/{name}")
    public ResponseEntity<?> getVirtualPlaylist(@PathVariable String name) {
        try {
            return ResponseEntity.ok(playlistService.load(name));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Requisição para salvar uma playlist virtual.
     *
     * @param name  nome da playlist.
     * @param paths lista de caminhos absolutos das músicas.
     */
    public record PlaylistSaveRequest(String name, List<String> paths) {}

    /**
     * Salva ou sobrescreve uma playlist virtual.
     *
     * @param request requisição com nome e caminhos das músicas.
     * @return "Saved" em caso de sucesso ou erro em caso de falha.
     */
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

    /**
     * Exclui uma playlist virtual.
     *
     * @param name nome da playlist a ser excluída.
     * @return "Deleted" em caso de sucesso ou 404 se não existir.
     */
    @DeleteMapping("/playlist/{name}")
    public ResponseEntity<?> deleteVirtualPlaylist(@PathVariable String name) {
        try {
            playlistService.delete(name);
            return ResponseEntity.ok("Deleted");
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Renomeia uma playlist virtual existente.
     *
     * @param request requisição com nome atual e novo nome.
     * @return "Renamed" em caso de sucesso ou 404 se não existir.
     */
    @PostMapping("/playlist/rename")
    public ResponseEntity<?> renameVirtualPlaylist(@RequestBody PlaylistRenameRequest request) {
        try {
            playlistService.rename(request.oldName(), request.newName());
            return ResponseEntity.ok("Renamed");
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Requisição para renomear uma playlist virtual.
     *
     * @param oldName nome atual da playlist.
     * @param newName novo nome da playlist.
     */
    public record PlaylistRenameRequest(String oldName, String newName) {}
}