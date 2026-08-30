package com.ovelha.fy.player.playlist.infrastructure;

import com.ovelha.fy.player.domain.model.MusicFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes do {@link FileMusicScanner}: percorre uma pasta real temporária e
 * verifica quais arquivos MP3 são coletados, recursivamente e por extensão.
 */
class FileMusicFileScannerTest {

    @TempDir
    Path dir;

    @Test
    void scansMp3FilesRecursivelyAndIgnoresOthers() throws IOException {
        Path sub = Files.createDirectory(dir.resolve("sub"));
        Files.write(dir.resolve("a.mp3"), new byte[] { 1 });
        Files.write(dir.resolve("b.MP3"), new byte[] { 1 });
        Files.write(dir.resolve("notaudio.txt"), new byte[] { 1 });
        Files.write(sub.resolve("c.mp3"), new byte[] { 1 });

        List<MusicFile> musicFiles = new FileMusicScanner().scanFolder(dir.toString());

        assertEquals(3, musicFiles.size());
        assertTrue(musicFiles.stream().allMatch(m -> m.getPath().toLowerCase().endsWith(".mp3")));
        assertTrue(musicFiles.stream().allMatch(m -> Path.of(m.getPath()).isAbsolute()));
        assertTrue(musicFiles.stream().noneMatch(m -> m.getPath().toLowerCase().contains("notaudio")));
    }

    @Test
    void throwsWhenFolderDoesNotExist() {
        assertThrows(IOException.class,
                () -> new FileMusicScanner().scanFolder(dir.resolve("missing").toString()));
    }

    @Test
    void throwsWhenPathIsNotADirectory() throws IOException {
        Path file = dir.resolve("file.mp3");
        Files.write(file, new byte[] { 1 });

        assertThrows(IOException.class, () -> new FileMusicScanner().scanFolder(file.toString()));
    }
}