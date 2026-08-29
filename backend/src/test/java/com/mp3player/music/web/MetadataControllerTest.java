package com.mp3player.music.web;

import com.mp3player.music.application.CoverService;
import com.mp3player.music.application.Id3Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetadataControllerTest {

    @Mock
    Id3Service id3Service;

    @Mock
    CoverService coverService;

    @Test
    void getId3ReturnsTagMap() {
        when(id3Service.getForFile("a.mp3")).thenReturn(Map.of("title", "Titulo"));
        MetadataController controller = new MetadataController(id3Service, coverService);

        var response = controller.getId3("a.mp3");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Titulo", response.getBody().get("title"));
    }

    @Test
    void updateId3ReturnsUpdatedTags() {
        when(id3Service.update("a.mp3", Map.of("title", "Novo")))
                .thenReturn(Map.of("title", "Novo"));
        MetadataController controller = new MetadataController(id3Service, coverService);

        var response = controller.updateId3(
                new MetadataController.Id3UpdateRequest("a.mp3", Map.of("title", "Novo")));

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> updated = (Map<?, ?>) response.getBody();
        assertEquals("Novo", updated.get("title"));
    }

    @Test
    void updateId3ReturnsBadRequestOnFailure() {
        when(id3Service.update("a.mp3", Map.of("title", "Novo")))
                .thenThrow(new RuntimeException("corrompido"));
        MetadataController controller = new MetadataController(id3Service, coverService);

        var response = controller.updateId3(
                new MetadataController.Id3UpdateRequest("a.mp3", Map.of("title", "Novo")));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Error: corrompido", response.getBody());
    }

    @Test
    void getBulkId3ReturnsMap() {
        when(id3Service.getBulk(List.of("a.mp3"), false))
                .thenReturn(Map.of("a.mp3", Map.of("title", "A")));
        MetadataController controller = new MetadataController(id3Service, coverService);

        var response = controller.getBulkId3(List.of("a.mp3"), false);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("A", response.getBody().get("a.mp3").get("title"));
    }

    @Test
    void getCoverReturnsNotFoundWhenPathHasNoParent() {
        MetadataController controller = new MetadataController(id3Service, coverService);

        var response = controller.getCover("song.mp3");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getCoverReturnsCoverFileFoundInFolder(@TempDir Path dir) throws IOException {
        Files.write(dir.resolve("cover.jpg"), new byte[] { 1, 2, 3 });
        MetadataController controller = new MetadataController(id3Service, coverService);
        String songPath = dir.resolve("song.mp3").toString();

        var response = controller.getCover(songPath);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.parseMediaType("image/jpeg"), response.getHeaders().getContentType());
    }

    @Test
    void getCoverReturnsNotFoundWhenNoCoverInFolder(@TempDir Path dir) throws IOException {
        Files.write(dir.resolve("song.mp3"), new byte[] { 1, 2, 3 });
        MetadataController controller = new MetadataController(id3Service, coverService);
        String songPath = dir.resolve("song.mp3").toString();

        var response = controller.getCover(songPath);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void downloadCoverWithBlankPathReturnsBadRequest() throws IOException {
        MetadataController controller = new MetadataController(id3Service, coverService);

        var response = controller.downloadCover(new MetadataController.CoverDownloadRequest("  "));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Caminho é obrigatório", response.getBody());
        verify(coverService, never()).download(anyString());
    }

    @Test
    void downloadCoverReturnsSavedPath() throws IOException {
        when(coverService.download("C:\\audio\\a.mp3")).thenReturn("C:\\audio\\cover.jpg");
        MetadataController controller = new MetadataController(id3Service, coverService);

        var response = controller.downloadCover(
                new MetadataController.CoverDownloadRequest("C:\\audio\\a.mp3"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("C:\\audio\\cover.jpg", response.getBody());
    }

    @Test
    void downloadCoverReturnsBadRequestOnFailure() throws IOException {
        doThrow(new IOException("nenhuma imagem encontrada"))
                .when(coverService).download("C:\\audio\\a.mp3");
        MetadataController controller = new MetadataController(id3Service, coverService);

        var response = controller.downloadCover(
                new MetadataController.CoverDownloadRequest("C:\\audio\\a.mp3"));

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().startsWith("Erro ao baixar capa: "));
        assertTrue(response.getBody().contains("nenhuma imagem encontrada"));
    }
}