package com.ovelha.fy.lyrics.web;

import com.ovelha.fy.lyrics.application.LyricsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LyricsControllerTest {

    @Mock
    LyricsService lyricsService;

    @Test
    void getCachedLyricsNotFound() {
        when(lyricsService.getCached("a.mp3")).thenReturn(null);
        LyricsController controller = new LyricsController(lyricsService);

        var response = controller.getCachedLyrics("a.mp3");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getCachedLyricsReturnsText() {
        when(lyricsService.getCached("a.mp3")).thenReturn("letra salva");
        LyricsController controller = new LyricsController(lyricsService);

        var response = controller.getCachedLyrics("a.mp3");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("letra salva", response.getBody());
    }

    @Test
    void getLyricsReturnsText() {
        when(lyricsService.get("a.mp3")).thenReturn("linha 1\nlinha 2");
        LyricsController controller = new LyricsController(lyricsService);

        var response = controller.getLyrics("a.mp3");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("linha 1\nlinha 2", response.getBody());
    }

    @Test
    void getLyricsReturnsBadRequestOnFailure() {
        doThrow(new IllegalStateException("pagina fora do ar")).when(lyricsService).get("a.mp3");
        LyricsController controller = new LyricsController(lyricsService);

        var response = controller.getLyrics("a.mp3");

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().startsWith("Erro ao buscar letra: "));
        assertTrue(response.getBody().contains("pagina fora do ar"));
    }

    @Test
    void saveWithMissingFieldsReturnsBadRequest() {
        LyricsController controller = new LyricsController(lyricsService);

        var response = controller.saveLyrics(new LyricsController.LyricsSaveRequest(null, "texto"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Caminho e texto são obrigatórios", response.getBody());
        verify(lyricsService, never()).save(anyString(), anyString());
    }

    @Test
    void saveReturnsOk() {
        LyricsController controller = new LyricsController(lyricsService);

        var response = controller.saveLyrics(new LyricsController.LyricsSaveRequest("a.mp3", "letra"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Letra salva", response.getBody());
        verify(lyricsService).save("a.mp3", "letra");
    }

    @Test
    void saveReturnsBadRequestOnFailure() {
        doThrow(new IllegalStateException("falha")).when(lyricsService).save("a.mp3", "letra");
        LyricsController controller = new LyricsController(lyricsService);

        var response = controller.saveLyrics(new LyricsController.LyricsSaveRequest("a.mp3", "letra"));

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().startsWith("Erro ao salvar letra: "));
    }

    @Test
    void deleteWithBlankPathReturnsBadRequest() {
        LyricsController controller = new LyricsController(lyricsService);

        var response = controller.deleteLyrics("  ");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Caminho é obrigatório", response.getBody());
        verify(lyricsService, never()).delete(anyString());
    }

    @Test
    void deleteReturnsOk() {
        LyricsController controller = new LyricsController(lyricsService);

        var response = controller.deleteLyrics("a.mp3");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Letra removida", response.getBody());
        verify(lyricsService).delete("a.mp3");
    }

    @Test
    void deleteReturnsBadRequestOnFailure() {
        doThrow(new IllegalStateException("falha")).when(lyricsService).delete("a.mp3");
        LyricsController controller = new LyricsController(lyricsService);

        var response = controller.deleteLyrics("a.mp3");

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().startsWith("Erro ao remover letra: "));
    }
}