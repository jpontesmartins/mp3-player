package com.ovelha.fy.player.music.infrastructure;

import com.ovelha.fy.player.domain.model.MusicFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes do {@link Id3MagicCodec} mp3agic: usa um MP3 silencioso real (frames
 * MPEG-1 Layer III preenchidos com zero) criado no diretório temporário, sem acessar
 * nenhuma rede ou recurso externo.
 */
class Id3MagicCodecTest {

    private static final int SILENT_FRAME_SIZE = 417;

    @TempDir
    Path dir;

    private Path writeSilentMp3(String name, int frames) throws IOException {
        Path file = dir.resolve(name);
        byte[] frame = new byte[SILENT_FRAME_SIZE];
        frame[0] = (byte) 0xFF;
        frame[1] = (byte) 0xFB;
        frame[2] = (byte) 0x90;
        frame[3] = 0x00;
        try (OutputStream out = Files.newOutputStream(file)) {
            for (int i = 0; i < frames; i++) {
                out.write(frame);
            }
        }
        return file;
    }

    @Test
    void readKeepsDurationAndBitrateWhenNoTagsPresent() throws IOException {
        Path file = writeSilentMp3("track.mp3", 4);
        Id3MagicCodec codec = new Id3MagicCodec();

        MusicFile musicFile = codec.read(file.toString());

        assertEquals(file.toString(), musicFile.getPath());
        assertNull(musicFile.getMetadata().title());
        assertNotNull(musicFile.getMetadata().durationMs());
        assertNotNull(musicFile.getMetadata().bitrateKbps());
    }

    @Test
    void readReturnsEmptyMetadataForUnreadableFile() {
        Id3MagicCodec codec = new Id3MagicCodec();

        MusicFile musicFile = codec.read(dir.resolve("missing.mp3").toString());

        assertEquals(dir.resolve("missing.mp3").toString(), musicFile.getPath());
        assertTrue(musicFile.toTagMap().isEmpty());
    }

    @Test
    void updateWritesTagsAndReadsThemBack() throws IOException {
        Path file = writeSilentMp3("tagged.mp3", 4);
        Id3MagicCodec codec = new Id3MagicCodec();
        Map<String, String> tags = Map.of(
                "title", "Novo Titulo",
                "artist", "Novo Artista",
                "album", "Novo Album",
                "genre", "Rock",
                "track", "3",
                "disc", "1",
                "year", "2010");

        MusicFile updated = codec.update(file.toString(), tags);
        MusicFile readBack = codec.read(file.toString());

        assertEquals("Novo Titulo", updated.getMetadata().title());
        assertEquals("Novo Titulo", readBack.getMetadata().title());
        assertEquals("Novo Artista", readBack.getMetadata().artist());
        assertEquals("Novo Album", readBack.getMetadata().album());
        assertEquals("Rock", readBack.getMetadata().genre());
        assertEquals("3", readBack.getMetadata().track());
        assertEquals("1", readBack.getMetadata().disc());
    }

    @Test
    void updateThrowsIllegalStateOnFailure() {
        Id3MagicCodec codec = new Id3MagicCodec();
        assertThrows(IllegalStateException.class,
                () -> codec.update(dir.resolve("missing.mp3").toString(), Map.of("title", "X")));
    }
}