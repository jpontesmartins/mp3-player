package com.mp3player.metadata.infrastructure;

import com.mp3player.metadata.infrastructure.CoverProperties;
import com.mp3player.metadata.domain.model.CoverImage;
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

class ItunesCoverSearcherTest {

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
    void findCoverReturnsItunesCoverWhenMatchFound() throws IOException {
        String itunesBody = "{\"results\":[{\"artworkUrl100\":\"https://example.com/art/100x100bb.jpg\"}]}";
        byte[] imageBytes = {1, 2, 3, 4};
        Connection.Response itunesResponse = responseMock(itunesBody, null, null);
        Connection.Response imageResponse = responseMock("", imageBytes, "image/jpeg");
        Connection itunesConnection = connectionMock(itunesResponse);
        Connection imageConnection = connectionMock(imageResponse);

        try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
            jsoup.when(() -> Jsoup.connect(anyString())).thenAnswer(invocation -> {
                String url = invocation.getArgument(0);
                return url.startsWith("https://example.com") ? imageConnection : itunesConnection;
            });

            CoverDownloader downloader = new CoverDownloader(DEFAULT_PROPS);
            ItunesCoverSearcher searcher = new ItunesCoverSearcher(downloader, DEFAULT_PROPS);

            CoverImage cover = searcher.findCover("Artista Album");

            assertNotNull(cover);
            assertArrayEquals(imageBytes, cover.bytes());
            assertEquals("image/jpeg", cover.contentType());
        }
    }

    @Test
    void findCoverDownloadsThe600x600Version() throws IOException {
        String itunesBody = "{\"results\":[{\"artworkUrl100\":\"https://example.com/art/100x100bb.jpg\"}]}";
        Connection.Response itunesResponse = responseMock(itunesBody, null, null);
        Connection.Response imageResponse = responseMock("", new byte[]{1, 2}, "image/jpeg");
        Connection itunesConnection = connectionMock(itunesResponse);
        Connection imageConnection = connectionMock(imageResponse);

        try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
            jsoup.when(() -> Jsoup.connect(anyString())).thenAnswer(invocation -> {
                String url = invocation.getArgument(0);
                return url.startsWith("https://example.com") ? imageConnection : itunesConnection;
            });

            CoverDownloader downloader = new CoverDownloader(DEFAULT_PROPS);
            ItunesCoverSearcher searcher = new ItunesCoverSearcher(downloader, DEFAULT_PROPS);

            searcher.findCover("Artista Album");

            ArgumentCaptor<String> urls = ArgumentCaptor.forClass(String.class);
            jsoup.verify(() -> Jsoup.connect(urls.capture()), atLeastOnce());
            assertTrue(urls.getAllValues().contains("https://example.com/art/600x600bb.jpg"));
        }
    }

    @Test
    void findCoverReturnsNullWhenNoMatch() throws IOException {
        try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
            Connection.Response itunesResponse = responseMock("{}", null, null);
            Connection itunesConnection = connectionMock(itunesResponse);
            jsoup.when(() -> Jsoup.connect(anyString())).thenReturn(itunesConnection);

            CoverDownloader downloader = new CoverDownloader(DEFAULT_PROPS);
            ItunesCoverSearcher searcher = new ItunesCoverSearcher(downloader, DEFAULT_PROPS);

            assertNull(searcher.findCover("Artista Album"));
        }
    }

    @Test
    void findCoverUrlEncodesSearchTerm() throws IOException {
        String itunesBody = "{\"results\":[{\"artworkUrl100\":\"https://example.com/art/100x100bb.jpg\"}]}";
        Connection.Response itunesResponse = responseMock(itunesBody, null, null);
        Connection.Response imageResponse = responseMock("", new byte[]{1, 2}, "image/jpeg");
        Connection itunesConnection = connectionMock(itunesResponse);
        Connection imageConnection = connectionMock(imageResponse);

        try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
            jsoup.when(() -> Jsoup.connect(anyString())).thenAnswer(invocation -> {
                String url = invocation.getArgument(0);
                return url.startsWith("https://example.com") ? imageConnection : itunesConnection;
            });

            CoverDownloader downloader = new CoverDownloader(DEFAULT_PROPS);
            ItunesCoverSearcher searcher = new ItunesCoverSearcher(downloader, DEFAULT_PROPS);

            searcher.findCover("Artista Album");

            ArgumentCaptor<String> urls = ArgumentCaptor.forClass(String.class);
            jsoup.verify(() -> Jsoup.connect(urls.capture()), atLeastOnce());
            String searchUrl = urls.getAllValues().get(0);
            assertEquals("https://itunes.apple.com/search?entity=album&limit=1&term=Artista+Album", searchUrl);
        }
    }
}
