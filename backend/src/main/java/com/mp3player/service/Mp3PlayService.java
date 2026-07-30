package com.mp3player.service;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class Mp3PlayService {

    private static final int FRAMES_PER_CHUNK = 1;
    private static final int SAMPLES_PER_FRAME = 1152;

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

    public void play(String filePath) throws FileNotFoundException {
        play(filePath, 0);
    }

    private void play(String filePath, long startPositionMillis) throws FileNotFoundException {
        stopCurrent();

        analyzeFile(filePath);
        readId3Tags(filePath);

        FileInputStream fis = new FileInputStream(filePath);

        int startFrame = 0;
        if (startPositionMillis > 0 && totalFrames > 0) {
            startFrame = (int) (startPositionMillis * sampleRate / (SAMPLES_PER_FRAME * 1000L));
            startFrame = Math.min(startFrame, totalFrames - 1);
            if (startFrame > 0) {
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
        } catch (JavaLayerException e) {
            throw new RuntimeException("Error creating player", e);
        }

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
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            } catch (JavaLayerException e) {
            } finally {
                newPlayer.close();
                if (this.player == newPlayer) {
                    this.playing = false;
                }
            }
        });
    }

    public void seekTo(long positionMillis) {
        if (currentFilePath == null) return;
        String path = currentFilePath;
        try {
            play(path, positionMillis);
        } catch (FileNotFoundException e) {
            // should not happen
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
            totalFrames = -1;
        }
    }

    public void pause() {
        this.paused = true;
        this.pauseStartNanos = System.nanoTime();
    }

    public void resume() {
        if (pauseStartNanos != 0) {
            totalPausedNanos += System.nanoTime() - pauseStartNanos;
            pauseStartNanos = 0;
        }
        this.paused = false;
    }

    public String getCurrentFilePath() {
        return currentFilePath;
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isPaused() {
        return paused;
    }

    public long getElapsedMillis() {
        if (playStartNanos == 0) return 0;
        long now = pauseStartNanos != 0 ? pauseStartNanos : System.nanoTime();
        return (now - playStartNanos - totalPausedNanos) / 1_000_000;
    }

    public long getTotalMillis() {
        if (totalFrames <= 0 || sampleRate <= 0) return 0;
        return (long) totalFrames * SAMPLES_PER_FRAME * 1000 / sampleRate;
    }

    public Map<String, String> getId3Tags() {
        return id3Tags;
    }

    public Map<String, String> getId3TagsForFile(String filePath) {
        Map<String, String> tags = new LinkedHashMap<>();
        try {
            Mp3File mp3file = new Mp3File(filePath);
            if (mp3file.hasId3v2Tag()) {
                ID3v2 id3 = mp3file.getId3v2Tag();
                putIfNotEmpty(tags, "title", id3.getTitle());
                putIfNotEmpty(tags, "artist", id3.getArtist());
                putIfNotEmpty(tags, "album", id3.getAlbum());
                putIfNotEmpty(tags, "year", id3.getYear());
                putIfNotEmpty(tags, "genre", id3.getGenreDescription());
                putIfNotEmpty(tags, "track", id3.getTrack());
            }
            if (tags.isEmpty() && mp3file.hasId3v1Tag()) {
                var id3 = mp3file.getId3v1Tag();
                putIfNotEmpty(tags, "title", id3.getTitle());
                putIfNotEmpty(tags, "artist", id3.getArtist());
                putIfNotEmpty(tags, "album", id3.getAlbum());
                putIfNotEmpty(tags, "year", id3.getYear());
                putIfNotEmpty(tags, "genre", id3.getGenreDescription());
                putIfNotEmpty(tags, "track", id3.getTrack());
            }
            long durationMs = mp3file.getLengthInMilliseconds();
            if (durationMs > 0) {
                tags.put("duration_ms", String.valueOf(durationMs));
            }
        } catch (Exception e) {
            tags.put("error", "Could not read ID3 tags");
        }
        if (tags.isEmpty()) {
            tags.put("title", filePath.substring(filePath.lastIndexOf('\\') + 1));
        }
        return tags;
    }

    private void readId3Tags(String filePath) {
        this.id3Tags = getId3TagsForFile(filePath);
        System.out.println("--- ID3 Tags ---");
        id3Tags.forEach((k, v) -> System.out.println(k + ": " + v));
        System.out.println("-----------------");
    }

    private static void putIfNotEmpty(Map<String, String> map, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            map.put(key, value.trim());
        }
    }

    public void stop() {
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
