package com.mp3player.application.player;

import com.mp3player.domain.model.Settings;
import com.mp3player.domain.port.PlayerEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    PlayerEngine engine;

    @Test
    void pauseDoesNothingWhenNotPlaying() {
        when(engine.isPlaying()).thenReturn(false);
        PlayerService service = new PlayerService(engine);
        assertEquals("No music playing", service.pause());
        verify(engine, never()).pause();
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
    void resumePausesMusicWhenValid() {
        when(engine.isPlaying()).thenReturn(true);
        when(engine.isPaused()).thenReturn(true);
        PlayerService service = new PlayerService(engine);
        assertEquals("Resumed", service.resume());
        verify(engine).resume();
    }

    @Test
    void nextInContinuousModeReturnsFollowingSong() {
        PlayerService service = new PlayerService(engine);
        assertEquals("b.mp3", service.next("a.mp3", List.of("a.mp3", "b.mp3", "c.mp3"), Settings.PlaybackMode.CONTINUOUS));
        assertEquals("c.mp3", service.next("b.mp3", List.of("a.mp3", "b.mp3", "c.mp3"), Settings.PlaybackMode.CONTINUOUS));
        assertEquals("a.mp3", service.next("c.mp3", List.of("a.mp3", "b.mp3", "c.mp3"), Settings.PlaybackMode.CONTINUOUS));
    }

    @Test
    void nextInRepeatModeReturnsSameSong() {
        PlayerService service = new PlayerService(engine);
        assertEquals("b.mp3", service.next("b.mp3", List.of("a.mp3", "b.mp3"), Settings.PlaybackMode.REPEAT));
    }

    @Test
    void previousAtFirstWrapsToLast() {
        PlayerService service = new PlayerService(engine);
        assertEquals("c.mp3", service.previous("a.mp3", List.of("a.mp3", "b.mp3", "c.mp3"), Settings.PlaybackMode.CONTINUOUS));
    }

    @Test
    void nextWithEmptyListReturnsNull() {
        PlayerService service = new PlayerService(engine);
        assertNull(service.next("a.mp3", List.of(), Settings.PlaybackMode.CONTINUOUS));
    }

    @Test
    void statusWhenStopped() {
        when(engine.getCurrentFilePath()).thenReturn(null);
        PlayerService service = new PlayerService(engine);
        assertEquals("stopped", service.status().get("status"));
    }
}