package com.mp3player.music.application;

import com.mp3player.music.domain.model.CoverImage;
import com.mp3player.player.domain.model.MusicFile;
import com.mp3player.music.domain.port.AlbumCoverSearcher;
import com.mp3player.music.domain.port.Id3Codec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do {@link CoverService}: o {@code Id3Codec} (tags) e o
 * {@code AlbumCoverSearcher} (web) são mockados, e a gravação da capa acontece
 * em um diretório temporário real para validar o caminho salvo e os bytes.
 */
@ExtendWith(MockitoExtension.class)
class CoverServiceTest {

    @Mock
    Id3Codec id3Codec;

    @Mock
    AlbumCoverSearcher coverSearcher;

    @TempDir
    Path dir;

    private MusicFile musicWith(String path, String artist, String album) {
        return new MusicFile(path, new MusicFile.Metadata("Titulo", artist, album, null, null, null, null));
    }

    @Test
    void downloadSavesCoverInAlbumFolderAndReturnsPath() throws IOException {
        String songPath = dir.resolve("song.mp3").toString();
        when(id3Codec.read(songPath)).thenReturn(musicWith(songPath, "Artista", "Álbum"));
        when(coverSearcher.findCover("Artista Álbum"))
                .thenReturn(new CoverImage(new byte[] { 1, 2, 3 }, "image/jpeg"));
        CoverService service = new CoverService(id3Codec, coverSearcher);

        String saved = service.download(songPath);

        Path expected = dir.resolve("cover.jpg");
        assertEquals(expected.toString(), saved);
        assertTrue(Files.exists(expected));
        assertArrayEquals(new byte[] { 1, 2, 3 }, Files.readAllBytes(expected));
    }

    @Test
    void downloadBuildsQueryFromArtistAndAlbum() throws IOException {
        String songPath = dir.resolve("song.mp3").toString();
        when(id3Codec.read(songPath)).thenReturn(musicWith(songPath, "Artista", "Álbum"));
        when(coverSearcher.findCover("Artista Álbum")).thenReturn(new CoverImage(new byte[] { 1 }, "image/jpeg"));
        CoverService service = new CoverService(id3Codec, coverSearcher);

        service.download(songPath);

        verify(coverSearcher).findCover("Artista Álbum");
    }

    @Test
    void downloadFallsBackToArtistFromFilenameWhenNoId3() throws IOException {
        String songPath = dir.resolve("Artist - Song.mp3").toString();
        when(id3Codec.read(songPath)).thenReturn(new MusicFile(songPath, MusicFile.Metadata.empty()));
        when(coverSearcher.findCover("Artist")).thenReturn(new CoverImage(new byte[] { 1 }, "image/jpeg"));
        CoverService service = new CoverService(id3Codec, coverSearcher);

        String saved = service.download(songPath);

        assertEquals(dir.resolve("cover.jpg").toString(), saved);
        verify(coverSearcher).findCover("Artist");
    }

    @Test
    void downloadUsesArtistOnlyWhenAlbumMatchesArtist() throws IOException {
        String songPath = dir.resolve("song.mp3").toString();
        when(id3Codec.read(songPath)).thenReturn(musicWith(songPath, "Metallica", "Metallica"));
        when(coverSearcher.findCover("Metallica")).thenReturn(new CoverImage(new byte[] { 1 }, "image/jpeg"));
        CoverService service = new CoverService(id3Codec, coverSearcher);

        service.download(songPath);

        verify(coverSearcher).findCover("Metallica");
        verify(coverSearcher, never()).findCover("Metallica Metallica");
    }

    @Test
    void downloadThrowsWhenNoImageFound() throws IOException {
        String songPath = dir.resolve("song.mp3").toString();
        when(id3Codec.read(songPath)).thenReturn(musicWith(songPath, "Artista", "Álbum"));
        when(coverSearcher.findCover("Artista Álbum")).thenReturn(null);
        CoverService service = new CoverService(id3Codec, coverSearcher);

        IOException e = assertThrows(IOException.class, () -> service.download(songPath));

        assertTrue(e.getMessage().contains("Artista Álbum"));
    }

    @Test
    void downloadThrowsWhenImageIsEmpty() throws IOException {
        String songPath = dir.resolve("song.mp3").toString();
        when(id3Codec.read(songPath)).thenReturn(musicWith(songPath, "Artista", "Álbum"));
        when(coverSearcher.findCover("Artista Álbum"))
                .thenReturn(new CoverImage(new byte[0], "image/jpeg"));
        CoverService service = new CoverService(id3Codec, coverSearcher);

        assertThrows(IOException.class, () -> service.download(songPath));
    }

    @Test
    void downloadThrowsWhenFolderCannotBeResolved() throws IOException {
        String rootPath = "C:\\";
        when(id3Codec.read(rootPath)).thenReturn(musicWith(rootPath, "Artista", "Álbum"));
        when(coverSearcher.findCover(anyString())).thenReturn(new CoverImage(new byte[] { 1 }, "image/jpeg"));
        CoverService service = new CoverService(id3Codec, coverSearcher);

        IOException e = assertThrows(IOException.class, () -> service.download(rootPath));

        assertTrue(e.getMessage().contains("Pasta do arquivo não encontrada"));
    }

    @Test
    void downloadMapsContentTypeToFileExtension() throws IOException {
        assertExtension("image/png", "png");
        assertExtension("image/webp", "webp");
        assertExtension("image/gif", "gif");
        assertExtension("image/jpeg", "jpg");
        assertExtension(null, "jpg");
    }

    @Test
    void downloadThrowsWhenId3CodecReadFails() {
        String songPath = dir.resolve("bad.mp3").toString();
        when(id3Codec.read(songPath)).thenThrow(new RuntimeException("arquivo corrompido"));
        CoverService service = new CoverService(id3Codec, coverSearcher);

        assertThrows(RuntimeException.class, () -> service.download(songPath));
    }

    @Test
    void downloadWithBothArtistAndAlbumBlankUsesFilenameFallback() throws IOException {
        String songPath = dir.resolve("Artist - Track.mp3").toString();
        when(id3Codec.read(songPath)).thenReturn(new MusicFile(songPath, MusicFile.Metadata.empty()));
        when(coverSearcher.findCover("Artist")).thenReturn(new CoverImage(new byte[] { 1 }, "image/jpeg"));
        CoverService service = new CoverService(id3Codec, coverSearcher);

        String saved = service.download(songPath);

        assertEquals(dir.resolve("cover.jpg").toString(), saved);
        verify(coverSearcher).findCover("Artist");
    }

    private void assertExtension(String contentType, String extension) throws IOException {
        String songPath = dir.resolve("album-no-" + extension).resolve("song.mp3").toString();
        Files.createDirectories(Path.of(songPath).getParent());
        when(id3Codec.read(songPath)).thenReturn(musicWith(songPath, "Artista", "Álbum"));
        when(coverSearcher.findCover("Artista Álbum")).thenReturn(new CoverImage(new byte[] { 1 }, contentType));
        CoverService service = new CoverService(id3Codec, coverSearcher);

        String saved = service.download(songPath);

        assertEquals(Path.of(songPath).getParent().resolve("cover." + extension).toString(), saved);
    }
}