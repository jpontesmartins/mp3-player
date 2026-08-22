package com.mp3player.player.infrastructure;

import com.mp3player.metadata.domain.port.Id3Codec;
import com.mp3player.player.domain.port.PlayerEngine;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Header;
import javazoom.jl.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

/**
 * Implementação do port {@link PlayerEngine} baseada em JLayer. Responsável pela
 * decodificação e reprodução de MP3, executada em uma virtual thread.
 */
@Component
public class JLayerPlayerEngine implements PlayerEngine {

    private static final Logger log = LoggerFactory.getLogger(JLayerPlayerEngine.class);

    private static final int FRAMES_PER_CHUNK = 1;
    private static final int SAMPLES_PER_FRAME = 1152;

    private final Id3Codec id3Codec;

    private volatile Player player;
    private volatile String currentFilePath;
    private volatile boolean paused;
    private volatile boolean playing;
    private volatile int sampleRate;
    private volatile int totalFrames;
    private volatile Map<String, String> id3Tags;
    private volatile long playStartNanos;
    private volatile long pauseStartNanos;
    private volatile long totalPausedNanos;

    public JLayerPlayerEngine(Id3Codec id3Codec) {
        this.id3Codec = id3Codec;
    }

    @Override
    public void play(String filePath) throws IOException {
        play(filePath, 0);
    }

    @Override
    public void play(String filePath, long startPositionMillis) throws IOException {
        stopCurrent();

        analyzeFile(filePath);
        readId3Tags(filePath);

        FileInputStream fis = new FileInputStream(filePath);

        int startFrame = 0;
        if (startPositionMillis > 0 && totalFrames > 0) {
            startFrame = (int) (startPositionMillis * sampleRate / (SAMPLES_PER_FRAME * 1000L));
            startFrame = Math.min(startFrame, totalFrames - 1);
            if (startFrame > 0) {
                log.info("[Player] Buscando frame {} (~{}ms)", startFrame, startPositionMillis);
                try {
                    Bitstream bitstream = new Bitstream(fis);
                    for (int i = 0; i < startFrame; i++) {
                        Header h = bitstream.readFrame();
                        if (h == null) break;
                        bitstream.closeFrame();
                    }
                    long bytePos = fis.getChannel().position();
                    bitstream.close();
                    fis.close();
                    fis = new FileInputStream(filePath);
                    fis.skip(bytePos);
                } catch (Exception e) {
                    throw new RuntimeException("Error seeking to position", e);
                }
            }
        }

        Player newPlayer;
        try {
            newPlayer = new Player(fis);
        } catch (javazoom.jl.decoder.JavaLayerException e) {
            throw new RuntimeException("Error creating player", e);
        }

        log.info("[Player] Reproduzindo: {} (início: {}ms)", filePath, startPositionMillis);
        this.currentFilePath = filePath;
        this.player = newPlayer;
        this.paused = false;
        this.playing = true;
        this.playStartNanos = System.nanoTime() - startPositionMillis * 1_000_000;
        this.pauseStartNanos = 0;
        this.totalPausedNanos = 0;

        Thread.startVirtualThread(() -> {
            try {
                while (playing && !newPlayer.isComplete()) {
                    if (!paused) {
                        newPlayer.play(FRAMES_PER_CHUNK);
                    } else {
                        // TODO: ver se isso aqui faz alguma diferença
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            } catch (javazoom.jl.decoder.JavaLayerException e) {
                log.warn("[Player] Erro na reprodução", e);
            } finally {
                newPlayer.close();
                if (this.player == newPlayer) {
                    this.playing = false;
                }
            }
        });
    }

    @Override
    public void seekTo(long positionMillis) {
        if (currentFilePath == null) return;
        log.info("[Player] Buscando posição {}ms", positionMillis);
        try {
            play(currentFilePath, positionMillis);
        } catch (Exception e) {
            log.error("[Player] Arquivo não encontrado ao buscar posição", e);
        }
    }

    private void analyzeFile(String filePath) {
        sampleRate = 44100;
        totalFrames = -1;
        try (FileInputStream fis = new FileInputStream(filePath)) {
            Bitstream bitstream = new Bitstream(fis);
            int count = 0;
            Header header;
            while ((header = bitstream.readFrame()) != null) {
                if (count == 0) {
                    sampleRate = header.frequency();
                }
                count++;
                bitstream.closeFrame();
            }
            bitstream.close();
            totalFrames = count;
        } catch (Exception e) {
            log.error("[Player] Falha ao analisar arquivo: {}", filePath, e);
            totalFrames = -1;
        }
    }

    @Override
    public void pause() {
        log.info("[Player] Pausado");
        this.paused = true;
        this.pauseStartNanos = System.nanoTime();
    }

    @Override
    public void resume() {
        log.info("[Player] Retomado");
        if (pauseStartNanos != 0) {
            totalPausedNanos += System.nanoTime() - pauseStartNanos;
            pauseStartNanos = 0;
        }
        this.paused = false;
    }

    @Override
    public String getCurrentFilePath() {
        return currentFilePath;
    }

    @Override
    public boolean isPlaying() {
        return playing;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public long getElapsedMillis() {
        if (playStartNanos == 0) return 0;
        long now = pauseStartNanos != 0 ? pauseStartNanos : System.nanoTime();
        return (now - playStartNanos - totalPausedNanos) / 1_000_000;
    }

    @Override
    public long getTotalMillis() {
        if (totalFrames <= 0 || sampleRate <= 0) return 0;
        return (long) totalFrames * SAMPLES_PER_FRAME * 1000 / sampleRate;
    }

    @Override
    public Map<String, String> getId3Tags() {
        return id3Tags;
    }

    private void readId3Tags(String filePath) {
        this.id3Tags = id3Codec.read(filePath).toTagMap();
        log.info("[Player] Tags ID3: {}", id3Tags);
    }

    @Override
    public void stop() {
        log.info("[Player] Parado");
        stopCurrent();
    }

    private void stopCurrent() {
        playing = false;
        paused = false;
        if (player != null) {
            player.close();
            player = null;
        }
        currentFilePath = null;
        totalFrames = -1;
        id3Tags = null;
        playStartNanos = 0;
        pauseStartNanos = 0;
        totalPausedNanos = 0;
    }
}