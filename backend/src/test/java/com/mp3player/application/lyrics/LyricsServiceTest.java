package com.mp3player.application.lyrics;

import com.mp3player.domain.model.Lyric;
import com.mp3player.domain.model.Music;
import com.mp3player.domain.port.Id3Codec;
import com.mp3player.domain.port.LyricsScraper;
import com.mp3player.domain.repository.LyricRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        Music music = new Music(path,
                new Music.Metadata("Song", "Artist", null, null, null, null, null));

        when(repository.find(path)).thenReturn(Optional.empty());
        when(id3Codec.read(path)).thenReturn(music);
        when(scraper.fetch("Artist", "Song")).thenReturn("linha1\nlinha2");

        LyricsService service = new LyricsService(id3Codec, scraper, repository);
        assertEquals("linha1\nlinha2", service.get(path));

        ArgumentCaptor<Lyric> captor = ArgumentCaptor.forClass(Lyric.class);
        verify(repository).save(captor.capture(), eq(music));
        assertEquals("linha1\nlinha2", captor.getValue().getText());
        assertEquals(path, captor.getValue().getMusicPath());
    }

    @Test
    void getFallsBackToFilenameWhenNoId3() throws IOException {
        String path = "C:\\OnlyFileName - Bd.mp3";
        Music music = new Music(path, Music.Metadata.empty());
        when(repository.find(path)).thenReturn(Optional.empty());
        when(id3Codec.read(path)).thenReturn(music);
        when(scraper.fetch("OnlyFileName", "Bd")).thenReturn("letra");

        LyricsService service = new LyricsService(id3Codec, scraper, repository);
        assertEquals("letra", service.get(path));
    }

    @Test
    void saveDelegatesToRepository() throws IOException {
        String path = "C:\\Artist - Song.mp3";
        Music music = new Music(path, new Music.Metadata("Song", "Artist", null, null, null, null, null));
        when(id3Codec.read(path)).thenReturn(music);

        LyricsService service = new LyricsService(id3Codec, scraper, repository);
        service.save(path, "minha letra");

        ArgumentCaptor<Lyric> captor = ArgumentCaptor.forClass(Lyric.class);
        verify(repository).save(captor.capture(), eq(music));
        assertEquals("minha letra", captor.getValue().getText());
        assertEquals(path, captor.getValue().getMusicPath());
    }

    @Test
    void deleteDelegatesToRepository() {
        LyricsService service = new LyricsService(id3Codec, scraper, repository);
        service.delete("C:\\song.mp3");
        verify(repository).delete("C:\\song.mp3");
    }
}