package com.mp3player.controller;

import com.mp3player.application.playlist.PlaylistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistControllerTest {

    @Mock
    PlaylistService playlistService;

    @Test
    void getPlaylistReturnsScannedFiles() throws IOException {
        when(playlistService.scanFolder("C:\\musica"))
                .thenReturn(List.of("C:\\musica\\a.mp3", "C:\\musica\\b.mp3"));
        PlaylistController controller = new PlaylistController(playlistService);

        var response = controller.getPlaylist("C:\\musica");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of("C:\\musica\\a.mp3", "C:\\musica\\b.mp3"), response.getBody());
    }

    @Test
    void getPlaylistReturnsBadRequestOnFailure() throws IOException {
        doThrow(new IOException("pasta invalida")).when(playlistService).scanFolder("C:\\x");
        PlaylistController controller = new PlaylistController(playlistService);

        var response = controller.getPlaylist("C:\\x");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Error: pasta invalida", response.getBody());
    }

    @Test
    void listPlaylistsReturnsOk() {
        when(playlistService.list()).thenReturn(List.of("Rock", "Pop"));
        PlaylistController controller = new PlaylistController(playlistService);

        var response = controller.listPlaylists();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of("Rock", "Pop"), response.getBody());
    }

    @Test
    void listPlaylistsReturnsBadRequestOnFailure() {
        doThrow(new IllegalStateException("falha")).when(playlistService).list();
        PlaylistController controller = new PlaylistController(playlistService);

        var response = controller.listPlaylists();

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Error: falha", response.getBody());
    }

    @Test
    void getVirtualPlaylistReturnsOk() {
        when(playlistService.load("Rock")).thenReturn(List.of("a.mp3"));
        PlaylistController controller = new PlaylistController(playlistService);

        var response = controller.getVirtualPlaylist("Rock");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of("a.mp3"), response.getBody());
    }

    @Test
    void getVirtualPlaylistNotFound() {
        doThrow(new IllegalArgumentException("nao existe")).when(playlistService).load("Rock");
        PlaylistController controller = new PlaylistController(playlistService);

        var response = controller.getVirtualPlaylist("Rock");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void saveWithoutNameReturnsBadRequest() {
        PlaylistController controller = new PlaylistController(playlistService);

        var response = controller.saveVirtualPlaylist(
                new PlaylistController.PlaylistSaveRequest("  ", List.of("a.mp3")));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Missing playlist name", response.getBody());
        verify(playlistService, never()).createOrUpdate(anyString(), any());
    }

    @Test
    void saveReturnsOk() {
        PlaylistController controller = new PlaylistController(playlistService);

        var response = controller.saveVirtualPlaylist(
                new PlaylistController.PlaylistSaveRequest("Rock", List.of("a.mp3", "b.mp3")));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Saved", response.getBody());
        verify(playlistService).createOrUpdate("Rock", List.of("a.mp3", "b.mp3"));
    }

    @Test
    void saveWithNullPathsSavesEmptyList() {
        PlaylistController controller = new PlaylistController(playlistService);

        var response = controller.saveVirtualPlaylist(
                new PlaylistController.PlaylistSaveRequest("Vazia", null));

        assertEquals(200, response.getStatusCode().value());
        verify(playlistService).createOrUpdate("Vazia", List.of());
    }

    @Test
    void saveReturnsBadRequestOnFailure() {
        doThrow(new IllegalStateException("falha"))
                .when(playlistService).createOrUpdate("Rock", List.of("a.mp3"));
        PlaylistController controller = new PlaylistController(playlistService);

        var response = controller.saveVirtualPlaylist(
                new PlaylistController.PlaylistSaveRequest("Rock", List.of("a.mp3")));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Error: falha", response.getBody());
    }

    @Test
    void deleteReturnsOk() {
        PlaylistController controller = new PlaylistController(playlistService);

        var response = controller.deleteVirtualPlaylist("Rock");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Deleted", response.getBody());
        verify(playlistService).delete("Rock");
    }

    @Test
    void deleteNotFound() {
        doThrow(new IllegalArgumentException("nao existe")).when(playlistService).delete("Rock");
        PlaylistController controller = new PlaylistController(playlistService);

        var response = controller.deleteVirtualPlaylist("Rock");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void renameReturnsOk() {
        PlaylistController controller = new PlaylistController(playlistService);

        var response = controller.renameVirtualPlaylist(
                new PlaylistController.PlaylistRenameRequest("Rock", "Classic"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Renamed", response.getBody());
        verify(playlistService).rename("Rock", "Classic");
    }

    @Test
    void renameNotFound() {
        doThrow(new IllegalArgumentException("nao existe"))
                .when(playlistService).rename("Rock", "Classic");
        PlaylistController controller = new PlaylistController(playlistService);

        var response = controller.renameVirtualPlaylist(
                new PlaylistController.PlaylistRenameRequest("Rock", "Classic"));

        assertEquals(404, response.getStatusCode().value());
    }
}