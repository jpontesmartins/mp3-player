package com.ovelha.fy.player.controls.web;

import com.ovelha.fy.player.controls.application.PlayerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerControllerTest {

    @Mock
    PlayerService playerService;

    @Test
    void playReturnsOk() throws IOException {
        PlayerController controller = new PlayerController(playerService);

        var response = controller.play("C:\\a.mp3");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Playing: C:\\a.mp3", response.getBody());
        verify(playerService).play("C:\\a.mp3");
    }

    @Test
    void playReturnsBadRequestOnFailure() throws IOException {
        doThrow(new IOException("arquivo nao encontrado")).when(playerService).play("C:\\x.mp3");
        PlayerController controller = new PlayerController(playerService);

        var response = controller.play("C:\\x.mp3");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Error: arquivo nao encontrado", response.getBody());
    }

    @Test
    void pauseReturnsOkWhenPaused() {
        when(playerService.pause()).thenReturn("Paused");
        PlayerController controller = new PlayerController(playerService);

        var response = controller.pause();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Paused", response.getBody());
        verify(playerService).pause();
    }

    @Test
    void pauseReturnsBadRequestWhenIgnored() {
        when(playerService.pause()).thenReturn("No music playing");
        PlayerController controller = new PlayerController(playerService);

        var response = controller.pause();

        assertEquals(400, response.getStatusCode().value());
        assertEquals("No music playing", response.getBody());
    }

    @Test
    void stopReturnsOk() {
        PlayerController controller = new PlayerController(playerService);

        var response = controller.stop();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Stopped", response.getBody());
        verify(playerService).stop();
    }

    @Test
    void seekWithMissingPositionReturnsBadRequest() {
        PlayerController controller = new PlayerController(playerService);

        var response = controller.seek(Map.of());

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Missing position", response.getBody());
        verify(playerService, never()).seekTo(anyLong());
    }

    @Test
    void seekReturnsOk() {
        when(playerService.seekTo(5000L)).thenReturn("Seeked to 5000");
        PlayerController controller = new PlayerController(playerService);

        var response = controller.seek(Map.of("position", 5000L));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Seeked to 5000", response.getBody());
        verify(playerService).seekTo(5000L);
    }

    @Test
    void seekReturnsBadRequestWhenIgnored() {
        when(playerService.seekTo(1L)).thenReturn("No music playing");
        PlayerController controller = new PlayerController(playerService);

        var response = controller.seek(Map.of("position", 1L));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("No music playing", response.getBody());
    }

    @Test
    void resumeReturnsOk() {
        when(playerService.resume()).thenReturn("Resumed");
        PlayerController controller = new PlayerController(playerService);

        var response = controller.resume();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Resumed", response.getBody());
        verify(playerService).resume();
    }

    @Test
    void resumeReturnsBadRequestWhenIgnored() {
        when(playerService.resume()).thenReturn("No music playing");
        PlayerController controller = new PlayerController(playerService);

        var response = controller.resume();

        assertEquals(400, response.getStatusCode().value());
        assertEquals("No music playing", response.getBody());
    }

    @Test
    void playingReturnsStatusMap() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "playing");
        when(playerService.status()).thenReturn(status);
        PlayerController controller = new PlayerController(playerService);

        var response = controller.playing();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("playing", response.getBody().get("status"));
    }
}