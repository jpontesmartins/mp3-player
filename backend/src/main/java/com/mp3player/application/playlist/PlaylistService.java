package com.mp3player.application.playlist;

import com.mp3player.domain.model.Playlist;
import com.mp3player.domain.port.MusicScanner;
import com.mp3player.domain.repository.PlaylistRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Service da aplicação para o módulo de playlist: carregar, criar, editar,
 * listar, excluir e renomear playlists, além do scan da pasta física.
 */
@Service
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final MusicScanner musicScanner;

    public PlaylistService(PlaylistRepository playlistRepository, MusicScanner musicScanner) {
        this.playlistRepository = playlistRepository;
        this.musicScanner = musicScanner;
    }

    /** Escaneia uma pasta física e retorna os caminhos absolutos de todas as músicas encontradas. */
    public List<String> scanFolder(String folderPath) throws IOException {
        return musicScanner.scanFolder(folderPath).stream().map(m -> m.getPath()).toList();
    }

    /** Lista as playlists salvas. */
    public List<String> list() {
        return playlistRepository.list();
    }

    /** Carrega os caminhos das músicas de uma playlist. */
    public List<String> load(String name) {
        return playlistRepository.load(name);
    }

    /** Cria ou sobrescreve uma playlist com os caminhos informados. */
    public void createOrUpdate(String name, List<String> songPaths) {
        playlistRepository.save(new Playlist(name, songPaths));
    }

    /** Exclui uma playlist. */
    public void delete(String name) {
        playlistRepository.delete(name);
    }

    /** Renomeia uma playlist. */
    public void rename(String currentName, String newName) {
        playlistRepository.rename(currentName, newName);
    }
}