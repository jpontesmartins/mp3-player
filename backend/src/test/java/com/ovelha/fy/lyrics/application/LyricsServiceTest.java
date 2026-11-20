package com.ovelha.fy.lyrics.application;

import com.ovelha.fy.lyrics.domain.model.Lyric;
import com.ovelha.fy.player.domain.model.MusicFile;
import com.ovelha.fy.player.music.domain.port.Id3Codec;
import com.ovelha.fy.lyrics.domain.port.LyricsScraper;
import com.ovelha.fy.lyrics.domain.repository.LyricRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LyricsServiceTest {

    @Mock
    Id3Codec id3Codec;
    @Mock
    LyricsScraper scraper;
    @Mock
    LyricRepository repository;

    @Test
    void getReturnsCachedLyricsWithoutScraping() throws IOException {
        String path = "C:\\A - B.mp3";
        when(repository.find(path)).thenReturn(Optional.of(new Lyric(path, "letras salvas")));

        LyricsService service = new LyricsService(id3Codec, scraper, repository);
        assertEquals("letras salvas", service.getCached(path));
        verify(scraper, never()).fetch(any(), any());
    }

    @Test
    void getFetchesAndSavesLyricsWhenNotCached() throws IOException {
        String path = "C:\\Artist - Song.mp3";
        MusicFile musicFile = new MusicFile(path,
                new MusicFile.Metadata("Song", "Artist", null, null, null, null, null));

        when(repository.find(path)).thenReturn(Optional.empty());
        when(id3Codec.read(path)).thenReturn(musicFile);
        when(scraper.fetch(List.of("Artist"), "Song")).thenReturn(Optional.of("linha1\nlinha2"));

        LyricsService service = new LyricsService(id3Codec, scraper, repository);
        assertEquals("linha1\nlinha2", service.get(path));

        ArgumentCaptor<Lyric> captor = ArgumentCaptor.forClass(Lyric.class);
        verify(repository).save(captor.capture(), eq(musicFile));
        assertEquals("linha1\nlinha2", captor.getValue().getText());
        assertEquals(path, captor.getValue().getMusicPath());
    }

    @Test
    void getFallsBackToFilenameWhenNoId3() throws IOException {
        String path = "C:\\OnlyFileName - Bd.mp3";
        MusicFile musicFile = new MusicFile(path, MusicFile.Metadata.empty());
        when(repository.find(path)).thenReturn(Optional.empty());
        when(id3Codec.read(path)).thenReturn(musicFile);
        when(scraper.fetch(List.of("OnlyFileName"), "Bd")).thenReturn(Optional.of("letra"));

        LyricsService service = new LyricsService(id3Codec, scraper, repository);
        assertEquals("letra", service.get(path));
    }

    @Test
    void saveDelegatesToRepository() throws IOException {
        String path = "C:\\Artist - Song.mp3";
        MusicFile musicFile = new MusicFile(path, new MusicFile.Metadata("Song", "Artist", null, null, null, null, null));
        when(id3Codec.read(path)).thenReturn(musicFile);

        LyricsService service = new LyricsService(id3Codec, scraper, repository);
        service.save(path, "minha letra");

        ArgumentCaptor<Lyric> captor = ArgumentCaptor.forClass(Lyric.class);
        verify(repository).save(captor.capture(), eq(musicFile));
        assertEquals("minha letra", captor.getValue().getText());
        assertEquals(path, captor.getValue().getMusicPath());
    }

    @Test
    void deleteDelegatesToRepository() {
        LyricsService service = new LyricsService(id3Codec, scraper, repository);
        service.delete("C:\\song.mp3");
        verify(repository).delete("C:\\song.mp3");
    }

    @Test
    void getCachedReturnsNullWhenNotCached() {
        when(repository.find("C:\\missing.mp3")).thenReturn(Optional.empty());

        LyricsService service = new LyricsService(id3Codec, scraper, repository);
        assertNull(service.getCached("C:\\missing.mp3"));
    }

    @Test
    void getReturnsCachedLyricsWithoutCallingScraper() throws IOException {
        String path = "C:\\A - B.mp3";
        when(repository.find(path)).thenReturn(Optional.of(new Lyric(path, "cached lyric")));

        LyricsService service = new LyricsService(id3Codec, scraper, repository);
        assertEquals("cached lyric", service.get(path));

        verify(scraper, never()).fetch(any(), any());
        verify(id3Codec, never()).read(any());
    }

    @Test
    void getReturnsFallbackMessageWhenScraperThrows() throws IOException {
        String path = "C:\\Artist - Song.mp3";
        MusicFile musicFile = new MusicFile(path, new MusicFile.Metadata("Song", "Artist", null, null, null, null, null));
        when(repository.find(path)).thenReturn(Optional.empty());
        when(id3Codec.read(path)).thenReturn(musicFile);
        when(scraper.fetch(List.of("Artist"), "Song")).thenThrow(new RuntimeException("network error"));

        LyricsService service = new LyricsService(id3Codec, scraper, repository);
        String result = service.get(path);

        assertEquals("Letra não encontrada para \"Song\" de Artist", result);
    }

    @Test
    void saveReadsId3AndDelegatesToRepository() throws IOException {
        String path = "C:\\Artist - Song.mp3";
        MusicFile musicFile = new MusicFile(path, new MusicFile.Metadata("Song", "Artist", null, null, null, null, null));
        when(id3Codec.read(path)).thenReturn(musicFile);

        LyricsService service = new LyricsService(id3Codec, scraper, repository);
        service.save(path, "my lyrics");

        ArgumentCaptor<Lyric> captor = ArgumentCaptor.forClass(Lyric.class);
        verify(repository).save(captor.capture(), eq(musicFile));
        assertEquals("my lyrics", captor.getValue().getText());
        assertEquals(path, captor.getValue().getMusicPath());
    }

    @Test
    void getDoesNotPersistNotFoundMessage() throws IOException {
        String path = "C:\\Artist - Song.mp3";
        MusicFile musicFile = new MusicFile(path, new MusicFile.Metadata("Song", "Artist", null, null, null, null, null));
        when(repository.find(path)).thenReturn(Optional.empty());
        when(id3Codec.read(path)).thenReturn(musicFile);
        when(scraper.fetch(List.of("Artist"), "Song")).thenReturn(Optional.empty());

        LyricsService service = new LyricsService(id3Codec, scraper, repository);

        assertEquals("Letra não encontrada para \"Song\" de Artist", service.get(path));
        verify(repository, never()).save(any(), any());
    }

    @Test
    void getPassesTagAndFilenameArtistsAsCandidates() throws IOException {
        String path = "C:\\musicas\\1-11 Gilberto Gil e Os Mutantes - Bat Macumba.mp3";
        MusicFile musicFile = new MusicFile(path,
                new MusicFile.Metadata("Bat Macumba", "Gilberto Gil", null, null, null, null, null));
        when(repository.find(path)).thenReturn(Optional.empty());
        when(id3Codec.read(path)).thenReturn(musicFile);
        when(scraper.fetch(List.of("Gilberto Gil", "Os Mutantes"), "Bat Macumba"))
                .thenReturn(Optional.of("letra"));

        LyricsService service = new LyricsService(id3Codec, scraper, repository);

        assertEquals("letra", service.get(path));
    }

    @Test
    void legacyNotFoundCacheIsIgnoredAndRefetched() throws IOException {
        String path = "C:\\Artist - Song.mp3";
        MusicFile musicFile = new MusicFile(path, new MusicFile.Metadata("Song", "Artist", null, null, null, null, null));
        when(repository.find(path)).thenReturn(Optional.of(
                new Lyric(path, "Letra não encontrada para \"Song\" de Artist")));
        when(id3Codec.read(path)).thenReturn(musicFile);
        when(scraper.fetch(List.of("Artist"), "Song")).thenReturn(Optional.of("letra nova"));

        LyricsService service = new LyricsService(id3Codec, scraper, repository);

        assertNull(service.getCached(path));
        assertEquals("letra nova", service.get(path));
        verify(repository).save(any(Lyric.class), eq(musicFile));
    }

    @Test
    void realLyricMentioningPhraseIsStillCached() {
        String path = "C:\\Artist - Song.mp3";
        String text = "Letra não encontrada para ninguém\nsegunda linha";
        when(repository.find(path)).thenReturn(Optional.of(new Lyric(path, text)));

        LyricsService service = new LyricsService(id3Codec, scraper, repository);

        assertEquals(text, service.getCached(path));
        assertTrue(Lyric.isNotFoundMessage("  Letra não encontrada para \"X\" de Y \n"));
    }
}
