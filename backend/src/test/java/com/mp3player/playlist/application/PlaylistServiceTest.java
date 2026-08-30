package com.mp3player.playlist.application;

import com.mp3player.player.domain.model.MusicFile;
import com.mp3player.playlist.domain.port.MusicScanner;
import com.mp3player.playlist.domain.repository.PlaylistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @Mock
    PlaylistRepository repository;
    @Mock
    MusicScanner scanner;

    @Test
    void listDelegatesToRepository() {
        when(repository.list()).thenReturn(List.of("Rock", "Pop"));
        PlaylistService service = new PlaylistService(repository, scanner);
        assertEquals(List.of("Rock", "Pop"), service.list());
    }

    @Test
    void scanFolderMapsMusicsToPaths() throws IOException {
        when(scanner.scanFolder("C:\\musica"))
                .thenReturn(List.of(new MusicFile("C:\\musica\\a.mp3", MusicFile.Metadata.empty()),
                        new MusicFile("C:\\musica\\b.mp3", MusicFile.Metadata.empty())));
        PlaylistService service = new PlaylistService(repository, scanner);
        assertEquals(List.of("C:\\musica\\a.mp3", "C:\\musica\\b.mp3"), service.scanFolder("C:\\musica"));
    }

    @Test
    void createOrUpdateSavesAsPlaylist() {
        PlaylistService service = new PlaylistService(repository, scanner);
        service.createOrUpdate("Rock", List.of("a.mp3", "b.mp3"));
        verify(repository).save(argThat(p -> "Rock".equals(p.getName())
                && p.getSongPaths().equals(List.of("a.mp3", "b.mp3"))));
    }

    @Test
    void loadDelegatesToRepository() {
        when(repository.load("Rock")).thenReturn(List.of("a.mp3", "b.mp3"));
        PlaylistService service = new PlaylistService(repository, scanner);
        assertEquals(List.of("a.mp3", "b.mp3"), service.load("Rock"));
    }

    @Test
    void scanFolderThrowsOnIoException() throws IOException {
        when(scanner.scanFolder("C:\\bad")).thenThrow(new IOException("acesso negado"));
        PlaylistService service = new PlaylistService(repository, scanner);
        assertThrows(IOException.class, () -> service.scanFolder("C:\\bad"));
    }

    @Test
    void deleteAndRenameDelegate() {
        PlaylistService service = new PlaylistService(repository, scanner);
        service.delete("Rock");
        service.rename("Rock", "Classic");
        verify(repository).delete("Rock");
        verify(repository).rename("Rock", "Classic");
    }

    @Test
    void scanFolderReturnsEmptyWhenScannerFindsNothing() throws IOException {
        when(scanner.scanFolder("C:\\empty")).thenReturn(Collections.emptyList());
        PlaylistService service = new PlaylistService(repository, scanner);

        assertTrue(service.scanFolder("C:\\empty").isEmpty());
    }

    @Test
    void loadReturnsEmptyWhenPlaylistDoesNotExist() {
        when(repository.load("NaoExiste")).thenReturn(Collections.emptyList());
        PlaylistService service = new PlaylistService(repository, scanner);

        assertTrue(service.load("NaoExiste").isEmpty());
    }

    @Test
    void createOrUpdateWithEmptyList() {
        PlaylistService service = new PlaylistService(repository, scanner);

        service.createOrUpdate("Empty", List.of());

        verify(repository).save(argThat(p -> "Empty".equals(p.getName())
                && p.getSongPaths().isEmpty()));
    }

    @Test
    void listReturnsEmptyWhenNoPlaylists() {
        when(repository.list()).thenReturn(Collections.emptyList());
        PlaylistService service = new PlaylistService(repository, scanner);

        assertTrue(service.list().isEmpty());
    }
}