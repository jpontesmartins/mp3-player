package com.mp3player.application.playlist;

import com.mp3player.domain.model.Music;
import com.mp3player.domain.port.MusicScanner;
import com.mp3player.domain.repository.PlaylistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistAppServiceTest {

    @Mock
    PlaylistRepository repository;
    @Mock
    MusicScanner scanner;

    @Test
    void listDelegatesToRepository() {
        when(repository.list()).thenReturn(List.of("Rock", "Pop"));
        PlaylistAppService service = new PlaylistAppService(repository, scanner);
        assertEquals(List.of("Rock", "Pop"), service.list());
    }

    @Test
    void scanFolderMapsMusicsToPaths() throws IOException {
        when(scanner.scanFolder("C:\\musica"))
                .thenReturn(List.of(new Music("C:\\musica\\a.mp3", Music.Metadata.empty()),
                        new Music("C:\\musica\\b.mp3", Music.Metadata.empty())));
        PlaylistAppService service = new PlaylistAppService(repository, scanner);
        assertEquals(List.of("C:\\musica\\a.mp3", "C:\\musica\\b.mp3"), service.scanFolder("C:\\musica"));
    }

    @Test
    void createOrUpdateSavesAsPlaylist() {
        PlaylistAppService service = new PlaylistAppService(repository, scanner);
        service.createOrUpdate("Rock", List.of("a.mp3", "b.mp3"));
        verify(repository).save(argThat(p -> "Rock".equals(p.getName())
                && p.getSongPaths().equals(List.of("a.mp3", "b.mp3"))));
    }

    @Test
    void deleteAndRenameDelegate() {
        PlaylistAppService service = new PlaylistAppService(repository, scanner);
        service.delete("Rock");
        service.rename("Rock", "Classic");
        verify(repository).delete("Rock");
        verify(repository).rename("Rock", "Classic");
    }
}