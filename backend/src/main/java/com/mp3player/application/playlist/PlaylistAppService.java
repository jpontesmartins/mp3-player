package com.mp3player.application.playlist;

import com.mp3player.domain.model.Playlist;
import com.mp3player.domain.port.MusicScanner;
import com.mp3player.domain.repository.PlaylistRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Application service for the file/library playlist module: loading, creating,
 * editing, listing, deleting and renaming playlists, plus the physical folder scan.
 */
@Service
public class PlaylistAppService {

    private final PlaylistRepository playlistRepository;
    private final MusicScanner musicScanner;

    public PlaylistAppService(PlaylistRepository playlistRepository, MusicScanner musicScanner) {
        this.playlistRepository = playlistRepository;
        this.musicScanner = musicScanner;
    }

    /** Scans a physical folder and returns the absolute paths of every music found. */
    public List<String> scanFolder(String folderPath) throws IOException {
        return musicScanner.scanFolder(folderPath).stream().map(m -> m.getPath()).toList();
    }

    public List<String> list() {
        return playlistRepository.list();
    }

    public List<String> load(String name) {
        return playlistRepository.load(name);
    }

    public void createOrUpdate(String name, List<String> songPaths) {
        playlistRepository.save(new Playlist(name, songPaths));
    }

    public void delete(String name) {
        playlistRepository.delete(name);
    }

    public void rename(String currentName, String newName) {
        playlistRepository.rename(currentName, newName);
    }
}