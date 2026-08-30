package com.ovelha.fy.player.music.infrastructure;

import com.ovelha.fy.player.music.domain.model.CoverImage;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DeezerCoverSearcherTest {

    private static final CoverProperties DEFAULT_PROPS = new CoverProperties(
            "Mozilla/5.0 (compatible; MP3Player/1.0)",
            "https://itunes.apple.com/search?entity=album&limit=1&term=",
            "https://api.deezer.com/search/album?limit=1&q=",
            15000,
            25000
    );

    private static Connection.Response responseMock(String body, byte[] imageBytes, String contentType) throws IOException {
        Connection.Response response = mock(Connection.Response.class);
        when(response.body()).thenReturn(body);
        if (imageBytes != null) when(response.bodyAsBytes()).thenReturn(imageBytes);
        if (contentType != null) when(response.contentType()).thenReturn(contentType);
        return response;
    }

    private static Connection connectionMock(Connection.Response response) throws IOException {
        Connection connection = mock(Connection.class);
        when(connection.userAgent(anyString())).thenReturn(connection);
        when(connection.timeout(anyInt())).thenReturn(connection);
        when(connection.ignoreContentType(anyBoolean())).thenReturn(connection);
        when(connection.execute()).thenReturn(response);
        return connection;
    }

    @Test
    void findCoverReturnsXlCoverWhenAvailable() throws IOException {
        String deezerBody = "{\"data\":[{\"cover_xl\":\"https://example.com/art/1000x1000.jpg\"}]}";
        byte[] imageBytes = {1, 2, 3, 4};
        Connection.Response deezerResponse = responseMock(deezerBody, null, null);
        Connection.Response imageResponse = responseMock("", imageBytes, "image/jpeg");
        Connection deezerConnection = connectionMock(deezerResponse);
        Connection imageConnection = connectionMock(imageResponse);

        try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
            jsoup.when(() -> Jsoup.connect(anyString())).thenAnswer(invocation -> {
                String url = invocation.getArgument(0);
                return url.startsWith("https://example.com") ? imageConnection : deezerConnection;
            });

            CoverDownloader downloader = new CoverDownloader(DEFAULT_PROPS);
            DeezerCoverSearcher searcher = new DeezerCoverSearcher(downloader, DEFAULT_PROPS);

            CoverImage cover = searcher.findCover("Artista Album");

            assertNotNull(cover);
            assertArrayEquals(imageBytes, cover.bytes());
        }
    }

    @Test
    void findCoverFallsBackToMediumWhenXlMissing() throws IOException {
        String deezerBody = "{\"data\":[{\"cover_medium\":\"https://example.com/art/250x250.jpg\"}]}";
        Connection.Response deezerResponse = responseMock(deezerBody, null, null);
        Connection.Response imageResponse = responseMock("", new byte[]{1, 2}, "image/jpeg");
        Connection deezerConnection = connectionMock(deezerResponse);
        Connection imageConnection = connectionMock(imageResponse);

        try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
            jsoup.when(() -> Jsoup.connect(anyString())).thenAnswer(invocation -> {
                String url = invocation.getArgument(0);
                return url.startsWith("https://example.com") ? imageConnection : deezerConnection;
            });

            CoverDownloader downloader = new CoverDownloader(DEFAULT_PROPS);
            DeezerCoverSearcher searcher = new DeezerCoverSearcher(downloader, DEFAULT_PROPS);

            CoverImage cover = searcher.findCover("Artista Album");

            assertNotNull(cover);
        }
    }

    @Test
    void findCoverReturnsNullWhenNoMatch() throws IOException {
        try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
            Connection.Response deezerResponse = responseMock("{}", null, null);
            Connection deezerConnection = connectionMock(deezerResponse);
            jsoup.when(() -> Jsoup.connect(anyString())).thenReturn(deezerConnection);

            CoverDownloader downloader = new CoverDownloader(DEFAULT_PROPS);
            DeezerCoverSearcher searcher = new DeezerCoverSearcher(downloader, DEFAULT_PROPS);

            assertNull(searcher.findCover("Artista Album"));
        }
    }

    @Test
    void findCoverUrlEncodesSearchTerm() throws IOException {
        String deezerBody = "{\"data\":[{\"cover_xl\":\"https://example.com/art/1000x1000.jpg\"}]}";
        Connection.Response deezerResponse = responseMock(deezerBody, null, null);
        Connection.Response imageResponse = responseMock("", new byte[]{1, 2}, "image/jpeg");
        Connection deezerConnection = connectionMock(deezerResponse);
        Connection imageConnection = connectionMock(imageResponse);

        try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
            jsoup.when(() -> Jsoup.connect(anyString())).thenAnswer(invocation -> {
                String url = invocation.getArgument(0);
                return url.startsWith("https://example.com") ? imageConnection : deezerConnection;
            });

            CoverDownloader downloader = new CoverDownloader(DEFAULT_PROPS);
            DeezerCoverSearcher searcher = new DeezerCoverSearcher(downloader, DEFAULT_PROPS);

            searcher.findCover("Artista Album");

            ArgumentCaptor<String> urls = ArgumentCaptor.forClass(String.class);
            jsoup.verify(() -> Jsoup.connect(urls.capture()), atLeastOnce());
            String searchUrl = urls.getAllValues().get(0);
            assertEquals("https://api.deezer.com/search/album?limit=1&q=Artista+Album", searchUrl);
        }
    }
}
