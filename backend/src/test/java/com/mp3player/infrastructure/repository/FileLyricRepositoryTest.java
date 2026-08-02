package com.mp3player.infrastructure.repository;

import com.mp3player.domain.model.Lyric;
import com.mp3player.domain.model.Music;
import com.mp3player.domain.port.Id3Codec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class FileLyricRepositoryTest {

    private Id3Codec id3Codec;
    private FileLyricRepository repository;

    @BeforeEach
    void setUp() {
        id3Codec = Mockito.mock(Id3Codec.class);
        repository = new FileLyricRepository(id3Codec);
    }

    private Music music(String path, String artist, String title) {
        return new Music(path, new Music.Metadata(title, artist, "Album", null, null, null, null));
    }

    @Test
    void saveAndFindRoundTripUsingId3Tags() throws IOException {
        Path dir = Files.createTempDirectory("lyrics-test");
        Path mp3 = dir.resolve("arquivo01.mp3"); // nome do arquivo não segue o padrão
        Files.write(mp3, new byte[0]);

        when(id3Codec.read(anyString())).thenReturn(music(mp3.toString(), "Artist", "Song"));

        Lyric lyric = new Lyric(mp3.toString(), "linha 1\nlinha 2");
        repository.save(lyric, null);

        assertTrue(repository.exists(mp3.toString()));
        var found = repository.find(mp3.toString());
        assertTrue(found.isPresent());
        assertEquals("linha 1\nlinha 2", found.get().getText());

        // the file is stored as "Artist - Song.txt" next to the audio (album folder)
        Path txt = dir.resolve("Artist - Song.txt");
        assertTrue(Files.exists(txt));
        assertEquals("linha 1\nlinha 2", Files.readString(txt, StandardCharsets.UTF_8));
    }

    @Test
    void fallsBackToFilenameWhenId3IsMissing() throws IOException {
        Path dir = Files.createTempDirectory("lyrics-test");
        Path mp3p = dir.resolve("Artist - Song.mp3");
        Files.write(mp3p, new byte[0]);

        when(id3Codec.read(anyString())).thenReturn(music(mp3p.toString(), null, null));

        Lyric lyric = new Lyric(mp3p.toString(), "texto");
        repository.save(lyric, null);

        Path txt = dir.resolve("Artist - Song.txt");
        assertTrue(Files.exists(txt));
    }

    @Test
    void findReturnsEmptyWhenLyricsWereNotSaved() throws IOException {
        Path mp3 = Files.createTempFile("no-lyrics", ".mp3");
        when(id3Codec.read(anyString())).thenReturn(music(mp3.toString(), null, null));

        assertFalse(repository.exists(mp3.toString()));
        assertTrue(repository.find(mp3.toString()).isEmpty());
    }
}