package com.mp3player.controller;

import com.mp3player.application.metadata.Id3Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InfoControllerTest {

    @Mock
    Id3Service id3Service;

    @Test
    void getInfoReturnsLogCacheAndPorts() {
        when(id3Service.cacheLocation()).thenReturn("C:\\cache\\metadata-cache.json");
        InfoController controller = new InfoController("C:\\log\\app.log", "8111", "8112", id3Service);

        var response = controller.getInfo();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("C:\\log\\app.log", response.getBody().get("logFile"));
        assertEquals("C:\\cache\\metadata-cache.json", response.getBody().get("cacheFile"));
        assertEquals("8111", response.getBody().get("backendPort"));
        assertEquals("8112", response.getBody().get("frontendPort"));
    }

    @Test
    void getInfoEchoesConfiguredValues() {
        when(id3Service.cacheLocation()).thenReturn("cache.json");
        InfoController controller = new InfoController("log.txt", "9000", "9001", id3Service);

        var response = controller.getInfo();

        assertEquals("log.txt", response.getBody().get("logFile"));
        assertEquals("cache.json", response.getBody().get("cacheFile"));
        assertEquals("9000", response.getBody().get("backendPort"));
        assertEquals("9001", response.getBody().get("frontendPort"));
    }
}