package com.mp3player.player.application;

import com.mp3player.player.domain.port.PlayerEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    PlayerEngine engine;

    @Test
    void playDelegatesToEngine() throws IOException {
        PlayerService service = new PlayerService(engine);

        service.play("a.mp3");

        verify(engine).play("a.mp3");
    }

    @Test
    void playPropagatesIOException() throws IOException {
        doThrow(new IOException("arquivo invalido")).when(engine).play("a.mp3");
        PlayerService service = new PlayerService(engine);

        IOException e = assertThrows(IOException.class, () -> service.play("a.mp3"));

        assertEquals("arquivo invalido", e.getMessage());
    }

    @Test
    void pauseReturnsNoMusicPlayingWhenNotPlaying() {
        when(engine.isPlaying()).thenReturn(false);
        PlayerService service = new PlayerService(engine);

        assertEquals("No music playing", service.pause());

        verify(engine, never()).pause();
    }

    @Test
    void pausePausesWhenPlaying() {
        when(engine.isPlaying()).thenReturn(true);
        PlayerService service = new PlayerService(engine);

        assertEquals("Paused", service.pause());

        verify(engine).pause();
    }

    @Test
    void resumeReturnsNoMusicPlayingWhenNotPlaying() {
        when(engine.isPlaying()).thenReturn(false);
        PlayerService service = new PlayerService(engine);

        assertEquals("No music playing", service.resume());

        verify(engine, never()).resume();
    }

    @Test
    void resumeRejectsWhenNotPaused() {
        when(engine.isPlaying()).thenReturn(true);
        when(engine.isPaused()).thenReturn(false);
        PlayerService service = new PlayerService(engine);

        assertEquals("Music is not paused", service.resume());

        verify(engine, never()).resume();
    }

    @Test
    void resumeResumesWhenPaused() {
        when(engine.isPlaying()).thenReturn(true);
        when(engine.isPaused()).thenReturn(true);
        PlayerService service = new PlayerService(engine);

        assertEquals("Resumed", service.resume());

        verify(engine).resume();
    }

    @Test
    void stopDelegatesToEngine() {
        PlayerService service = new PlayerService(engine);

        service.stop();

        verify(engine).stop();
    }

    @Test
    void seekReturnsNoMusicPlayingWithoutFile() {
        when(engine.getCurrentFilePath()).thenReturn(null);
        PlayerService service = new PlayerService(engine);

        assertEquals("No music playing", service.seekTo(1000));

        verify(engine, never()).seekTo(anyLong());
    }

    @Test
    void seekDelegatesToEngineWhenFilePlaying() {
        when(engine.getCurrentFilePath()).thenReturn("a.mp3");
        PlayerService service = new PlayerService(engine);

        assertEquals("Seeked to 5000", service.seekTo(5000));

        verify(engine).seekTo(5000);
    }

    @Test
    void statusStoppedWhenNoFile() {
        when(engine.getCurrentFilePath()).thenReturn(null);
        Map<String, Object> status = new PlayerService(engine).status();

        assertEquals("stopped", status.get("status"));
        assertEquals("", status.get("file"));
    }

    @Test
    void statusStoppedWhenNotPlaying() {
        when(engine.getCurrentFilePath()).thenReturn("a.mp3");
        when(engine.isPlaying()).thenReturn(false);

        Map<String, Object> status = new PlayerService(engine).status();

        assertEquals("stopped", status.get("status"));
    }

    @Test
    void statusPlayingInformsFilePositionDurationAndTags() {
        when(engine.getCurrentFilePath()).thenReturn("a.mp3");
        when(engine.isPlaying()).thenReturn(true);
        when(engine.isPaused()).thenReturn(false);
        when(engine.getElapsedMillis()).thenReturn(1200L);
        when(engine.getTotalMillis()).thenReturn(200000L);
        when(engine.getId3Tags()).thenReturn(Map.of("title", "A"));

        Map<String, Object> status = new PlayerService(engine).status();

        assertEquals("playing", status.get("status"));
        assertEquals("a.mp3", status.get("file"));
        assertEquals(1200L, status.get("position"));
        assertEquals(200000L, status.get("duration"));
        assertEquals(Map.of("title", "A"), status.get("id3"));
    }

    @Test
    void statusPausedWhenPaused() {
        when(engine.getCurrentFilePath()).thenReturn("a.mp3");
        when(engine.isPlaying()).thenReturn(true);
        when(engine.isPaused()).thenReturn(true);

        Map<String, Object> status = new PlayerService(engine).status();

        assertEquals("paused", status.get("status"));
    }

    @Test
    void stopAlwaysDelegatesEvenWhenNotPlaying() {
        PlayerService service = new PlayerService(engine);

        service.stop();

        verify(engine).stop();
    }

    @Test
    void seekToWithNegativePositionStillDelegates() {
        when(engine.getCurrentFilePath()).thenReturn("a.mp3");
        PlayerService service = new PlayerService(engine);

        assertEquals("Seeked to -1000", service.seekTo(-1000));

        verify(engine).seekTo(-1000);
    }

    @Test
    void statusShowsFileButStoppedWhenFilePathSetButNotPlaying() {
        when(engine.getCurrentFilePath()).thenReturn("a.mp3");
        when(engine.isPlaying()).thenReturn(false);

        Map<String, Object> status = new PlayerService(engine).status();

        assertEquals("stopped", status.get("status"));
        assertEquals("", status.get("file"));
        assertNull(status.get("position"));
        assertNull(status.get("duration"));
        assertNull(status.get("id3"));
    }

    @Test
    void playWithNullFilePath() throws IOException {
        PlayerService service = new PlayerService(engine);

        service.play(null);

        verify(engine).play((String) null);
    }

    @Test
    void pauseReturnsPausedAndEngineIsCalledExactlyOnce() {
        when(engine.isPlaying()).thenReturn(true);
        PlayerService service = new PlayerService(engine);

        assertEquals("Paused", service.pause());
        assertEquals("Paused", service.pause());

        verify(engine, times(2)).pause();
    }
}