package com.mp3player.infrastructure.audio;

import com.mp3player.domain.model.Music;
import com.mp3player.domain.port.Id3Codec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do {@link JLayerPlayerEngine}. Como o engine decodifica e toca
 * em uma virtual thread, geramos um fixture de MP3 silencioso real (frames MPEG-1
 * Layer III de 417 bytes preenchidos com zero) em um diretório temporário: o JLayer
 * o decodifica como silêncio, sem depender de arquivos de áudio no repositório.
 * O {@code Id3Codec} é mockado para isolar apenas a leitura de tags.
 */
@ExtendWith(MockitoExtension.class)
class JLayerPlayerEngineTest {

    private static final int SILENT_FRAME_SIZE = 417;

    @Mock
    Id3Codec id3Codec;

    @TempDir
    Path dir;

    /**
     * Escreve um MP3 com {@code frames} quadros MPEG-1 Layer III (128 kbps, 44.1 kHz)
     * preenchidos com zero. O cabeçalho fixo {@code FF FB 90 00} é de um frame válido
     * sem CRC; o tamanho 417 vem de 144 * 128000 / 44100 e o conteúdo zerado decodifica
     * como silêncio no JLayer.
     */
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

    /**
     * Tags ID3 falsas retornadas pelo mock do codec, sempre associadas ao caminho tocado.
     */
    private Music musicTagsFor(String path, String title) {
        return new Music(path, new Music.Metadata(title, "Artista", "Álbum", null, null, null, null, 200_000L, null));
    }

    /**
     * Verifica o estado inicial de um engine recém-criado: nada tocando, sem arquivo
     * corrente, sem tags ID3 e com os tempos de duração/posição zerados.
     */
    @Test
    void isIdleBeforeAnyPlayback() {
        JLayerPlayerEngine engine = new JLayerPlayerEngine(id3Codec);

        assertFalse(engine.isPlaying());
        assertFalse(engine.isPaused());
        assertNull(engine.getCurrentFilePath());
        assertNull(engine.getId3Tags());
        assertEquals(0, engine.getElapsedMillis());
        assertEquals(0, engine.getTotalMillis());
    }

    /**
     * Garante que chamar {@code stop()} sem haver reprodução não lança exceção nem
     * corrompe o estado interno do engine.
     */
    @Test
    void stopWhenIdleIsSafe() {
        JLayerPlayerEngine engine = new JLayerPlayerEngine(id3Codec);

        engine.stop();

        assertFalse(engine.isPlaying());
        assertFalse(engine.isPaused());
        assertNull(engine.getCurrentFilePath());
        assertEquals(0, engine.getTotalMillis());
    }

    /**
     * Caminho de erro do {@code play}: com arquivo inexistente o método lança
     * {@link FileNotFoundException} e o engine permanece ocioso. O mock confirma
     * que o codec foi consultado para as tags antes de a abertura do arquivo falhar.
     */
    @Test
    void playWithMissingFileThrowsAndKeepsEngineIdle() {
        when(id3Codec.read("C:\\missing.mp3")).thenReturn(musicTagsFor("C:\\missing.mp3", "X"));
        JLayerPlayerEngine engine = new JLayerPlayerEngine(id3Codec);

        assertThrows(FileNotFoundException.class, () -> engine.play("C:\\missing.mp3"));

        assertFalse(engine.isPlaying());
        assertNull(engine.getCurrentFilePath());
        verify(id3Codec).read("C:\\missing.mp3");
    }

    /**
     * Caminho feliz: com um MP3 válido o engine entra em reprodução, expõe o arquivo
     * corrente, a duração total estimada (derivada da análise de frames) e as tags ID3.
     * O {@code stop()} seguinte demonstra que toda a informação é limpa.
     */
    @Test
    void playWithValidFileStartsPlayback() throws IOException {
        Path file = writeSilentMp3("track.mp3", 8);
        when(id3Codec.read(file.toString())).thenReturn(musicTagsFor(file.toString(), "Titulo"));
        JLayerPlayerEngine engine = new JLayerPlayerEngine(id3Codec);

        engine.play(file.toString());

        assertTrue(engine.isPlaying());
        assertEquals(file.toString(), engine.getCurrentFilePath());
        assertTrue(engine.getTotalMillis() > 0);
        assertEquals("Titulo", engine.getId3Tags().get("title"));
        assertTrue(engine.getElapsedMillis() >= 0);

        engine.stop();
        assertFalse(engine.isPlaying());
        assertNull(engine.getCurrentFilePath());
        assertEquals(0, engine.getTotalMillis());
    }

    /**
     * Exercita a sobrecarga {@code play(file, startMillis)}, que percorre o Bitstream
     * pulando frames até a posição pedida antes de criar o player.
     */
    @Test
    void playWithStartPositionStillPlays() throws IOException {
        Path file = writeSilentMp3("track.mp3", 8);
        when(id3Codec.read(file.toString())).thenReturn(musicTagsFor(file.toString(), "Titulo"));
        JLayerPlayerEngine engine = new JLayerPlayerEngine(id3Codec);

        engine.play(file.toString(), 50);

        assertEquals(file.toString(), engine.getCurrentFilePath());
        engine.stop();
    }

    /**
     * Espera a virtual thread do JLayer chegar ao fim do arquivo: com poucos frames a
     * reprodução termina sozinha e {@code isPlaying()} volta a {@code false} sem
     * nenhuma chamada explícita de {@code stop()}.
     */
    @Test
    void playbackCompletesNaturally() throws Exception {
        Path file = writeSilentMp3("short.mp3", 4);
        when(id3Codec.read(file.toString())).thenReturn(musicTagsFor(file.toString(), "Curta"));
        JLayerPlayerEngine engine = new JLayerPlayerEngine(id3Codec);

        engine.play(file.toString());

        long deadline = System.currentTimeMillis() + 10_000;
        while (engine.isPlaying() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertFalse(engine.isPlaying(), "reprodução deveria terminar sozinha");
    }

    /**
     * Máquina de estados pausa/retoma: {@code pause()} marca {@code isPaused()} e
     * {@code resume()} desmarca, mesmo sem haver um áudio tocando.
     */
    @Test
    void pauseAndResumeTogglePausedState() {
        JLayerPlayerEngine engine = new JLayerPlayerEngine(id3Codec);

        engine.pause();
        assertTrue(engine.isPaused());

        engine.resume();
        assertFalse(engine.isPaused());
    }

    /**
     * Garante que {@code seekTo()} sem música tocando é um no-op: não lança exceção
     * e não altera o estado (sem arquivo corrente e engine parado).
     */
    @Test
    void seekToWithoutFilePlayingDoesNothing() {
        JLayerPlayerEngine engine = new JLayerPlayerEngine(id3Codec);

        engine.seekTo(1000);

        assertNull(engine.getCurrentFilePath());
        assertFalse(engine.isPlaying());
    }
}