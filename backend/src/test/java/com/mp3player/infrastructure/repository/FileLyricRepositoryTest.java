package com.mp3player.infrastructure.repository;

import com.mp3player.domain.model.Lyric;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileLyricRepositoryTest {

    @Test
    void saveAndFindRoundTrip() throws IOException {
        Path dir = Files.createTempDirectory("lyrics-test");
        Path mp3 = dir.resolve("Artist - Song.mp3");
        Files.write(mp3, new byte[0]);

        FileLyricRepository repository = new FileLyricRepository();
        Lyric lyric = new Lyric(mp3.toString(), "linha 1\nlinha 2");

        repository.save(lyric, null);

        assertTrue(repository.exists(mp3.toString()));
        var found = repository.find(mp3.toString());
        assertTrue(found.isPresent());
        assertEquals("linha 1\nlinha 2", found.get().getText());

        // the file is stored as "Artist - Song.txt" next to the audio
        Path txt = dir.resolve("Artist - Song.txt");
        assertTrue(Files.exists(txt));
        assertEquals("linha 1\nlinha 2", Files.readString(txt, StandardCharsets.UTF_8));
    }

    @Test
    void findReturnsEmptyWhenLyricsWereNotSaved() throws IOException {
        Path mp3 = Files.createTempFile("no-lyrics", ".mp3");
        FileLyricRepository repository = new FileLyricRepository();

        assertFalse(repository.exists(mp3.toString()));
        assertTrue(repository.find(mp3.toString()).isEmpty());
    }
}