package com.mp3player.infrastructure.cover;

import com.mp3player.config.CoverProperties;
import com.mp3player.domain.model.CoverImage;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do {@link MusicAlbumCoverSearcher}. O {@code Jsoup.connect} é
 * mockado estaticamente, então nenhuma chamada real às APIs do iTunes/Deezer ou
 * download de imagem acontecem: as respostas JSON e os bytes das capas são
 * simulados conforme a URL acessada.
 */
class MusicAlbumCoverSearcherTest {

    private static final CoverProperties DEFAULT_PROPS = new CoverProperties(
            "Mozilla/5.0 (compatible; MP3Player/1.0)",
            "https://itunes.apple.com/search?entity=album&limit=1&term=",
            "https://api.deezer.com/search/album?limit=1&q=",
            15000,
            25000
    );

    /** Resposta HTTP simulada com corpo de busca, bytes e content-type da capa. */
    private static Connection.Response responseMock(String body, byte[] imageBytes, String contentType) throws IOException {
        Connection.Response response = mock(Connection.Response.class);
        when(response.body()).thenReturn(body);
        if (imageBytes != null) {
            when(response.bodyAsBytes()).thenReturn(imageBytes);
        }
        if (contentType != null) {
            when(response.contentType()).thenReturn(contentType);
        }
        return response;
    }

    /** Conexão simulada com a cadeia de configuração e o execute retornando a resposta. */
    private static Connection connectionMock(Connection.Response response) throws IOException {
        Connection connection = mock(Connection.class);
        when(connection.userAgent(anyString())).thenReturn(connection);
        when(connection.timeout(anyInt())).thenReturn(connection);
        when(connection.ignoreContentType(anyBoolean())).thenReturn(connection);
        when(connection.execute()).thenReturn(response);
        return connection;
    }

    /**
     * Instala o mock estático do Jsoup roteando cada URL para a resposta certa:
     * busca no iTunes, busca no Deezer e download da capa.
     */
    private static void routeJsoup(MockedStatic<Jsoup> jsoup, String itunesBody, String deezerBody, String imageUrl) throws IOException {
        Connection.Response itunesResponse = responseMock(itunesBody, null, null);
        Connection.Response deezerResponse = responseMock(deezerBody, null, null);
        byte[] imageBytes = { 1, 2, 3, 4 };
        Connection.Response imageResponse = responseMock("", imageBytes, "image/jpeg");
        Connection itunesConnection = connectionMock(itunesResponse);
        Connection deezerConnection = connectionMock(deezerResponse);
        Connection imageConnection = connectionMock(imageResponse);

        jsoup.when(() -> Jsoup.connect(anyString())).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            if (imageUrl != null && url.startsWith(imageUrl)) return imageConnection;
            if (url.contains("itunes.apple.com")) return itunesConnection;
            return deezerConnection;
        });
    }

    @Test
    void findCoverReturnsItunesCoverWhenMatchFound() throws IOException {
        String itunesBody = "{\"results\":[{\"artworkUrl100\":\"https://example.com/art/100x100bb.jpg\"}]}";
        try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
            routeJsoup(jsoup, itunesBody, null, "https://example.com/art/600x600bb.jpg");
            MusicAlbumCoverSearcher searcher = new MusicAlbumCoverSearcher(DEFAULT_PROPS);

            CoverImage cover = searcher.findCover("Artista Album");

            assertNotNull(cover);
            assertArrayEquals(new byte[] { 1, 2, 3, 4 }, cover.bytes());
            assertEquals("image/jpeg", cover.contentType());
        }
    }

    @Test
    void findCoverDownloadsThe600x600VersionFromItunes() throws IOException {
        String itunesBody = "{\"results\":[{\"artworkUrl100\":\"https://example.com/art/100x100bb.jpg\"}]}";
        try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
            routeJsoup(jsoup, itunesBody, null, "https://example.com/art/600x600bb.jpg");
            MusicAlbumCoverSearcher searcher = new MusicAlbumCoverSearcher(DEFAULT_PROPS);

            searcher.findCover("Artista Album");

            ArgumentCaptor<String> urls = ArgumentCaptor.forClass(String.class);
            jsoup.verify(() -> Jsoup.connect(urls.capture()), atLeastOnce());
            List<String> visited = urls.getAllValues();
            assertTrue(visited.contains("https://example.com/art/600x600bb.jpg"),
                    "deveria baixar a versão 600x600: " + visited);
            assertEquals(2, visited.size(), "só deve existir busca no iTunes e o download");
        }
    }

    @Test
    void findCoverFallsBackToDeezerWhenItunesHasNoMatch() throws IOException {
        String deezerBody = "{\"data\":[{\"cover_xl\":\"https://example.com/art/1000x1000.jpg\"}]}";
        try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
            routeJsoup(jsoup, "{}", deezerBody, "https://example.com/art/1000x1000.jpg");
            MusicAlbumCoverSearcher searcher = new MusicAlbumCoverSearcher(DEFAULT_PROPS);

            CoverImage cover = searcher.findCover("Artista Album");

            assertNotNull(cover);
            assertEquals("image/jpeg", cover.contentType());

            ArgumentCaptor<String> urls = ArgumentCaptor.forClass(String.class);
            jsoup.verify(() -> Jsoup.connect(urls.capture()), atLeastOnce());
            List<String> visited = urls.getAllValues();
            assertTrue(visited.stream().anyMatch(u -> u.contains("itunes.apple.com")));
            assertTrue(visited.stream().anyMatch(u -> u.contains("api.deezer.com")));
        }
    }

    @Test
    void findCoverUsesDeezerMediumWhenXlMissing() throws IOException {
        String deezerBody = "{\"data\":[{\"cover_medium\":\"https://example.com/art/250x250.jpg\"}]}";
        try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
            routeJsoup(jsoup, "{}", deezerBody, "https://example.com/art/250x250.jpg");
            MusicAlbumCoverSearcher searcher = new MusicAlbumCoverSearcher(DEFAULT_PROPS);

            CoverImage cover = searcher.findCover("Artista Album");

            assertNotNull(cover);
        }
    }

    @Test
    void findCoverReturnsNullWhenNeitherProviderMatches() throws IOException {
        try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
            routeJsoup(jsoup, "{}", "{}", null);
            MusicAlbumCoverSearcher searcher = new MusicAlbumCoverSearcher(DEFAULT_PROPS);

            CoverImage cover = searcher.findCover("Artista Album");

            assertNull(cover);

            ArgumentCaptor<String> urls = ArgumentCaptor.forClass(String.class);
            jsoup.verify(() -> Jsoup.connect(urls.capture()), atMost(2));
            assertEquals(2, urls.getAllValues().size(), "deve consultar iTunes e Deezer");
        }
    }

    @Test
    void findCoverUrlEncodesSearchTerm() throws IOException {
        String itunesBody = "{\"results\":[{\"artworkUrl100\":\"https://example.com/art/100x100bb.jpg\"}]}";
        try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
            routeJsoup(jsoup, itunesBody, null, "https://example.com/art/600x600bb.jpg");
            MusicAlbumCoverSearcher searcher = new MusicAlbumCoverSearcher(DEFAULT_PROPS);

            searcher.findCover("Artista Album");

            ArgumentCaptor<String> urls = ArgumentCaptor.forClass(String.class);
            jsoup.verify(() -> Jsoup.connect(urls.capture()), atLeastOnce());
            String searchUrl = urls.getAllValues().get(0);
            assertEquals("https://itunes.apple.com/search?entity=album&limit=1&term=Artista+Album", searchUrl);
        }
    }

    @Test
    void findCoverReturnsNullWhenDownloadHasEmptyBody() throws IOException {
        try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
            Connection.Response itunesResponse = responseMock(
                    "{\"results\":[{\"artworkUrl100\":\"https://example.com/art/100x100bb.jpg\"}]}", null, null);
            Connection.Response imageResponse = responseMock("", new byte[0], "image/jpeg");
            Connection itunesConnection = connectionMock(itunesResponse);
            Connection imageConnection = connectionMock(imageResponse);
            jsoup.when(() -> Jsoup.connect(anyString())).thenAnswer(invocation -> {
                String url = invocation.getArgument(0);
                return url.startsWith("https://example.com") ? imageConnection : itunesConnection;
            });

            MusicAlbumCoverSearcher searcher = new MusicAlbumCoverSearcher(DEFAULT_PROPS);

            assertNull(searcher.findCover("Artista Album"));
        }
    }

    @Test
    void findCoverDefaultsToJpegWhenContentTypeIsNull() throws IOException {
        try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
            Connection.Response itunesResponse = responseMock(
                    "{\"results\":[{\"artworkUrl100\":\"https://example.com/art/100x100bb.jpg\"}]}", null, null);
            Connection.Response imageResponse = responseMock("", new byte[]{1, 2}, null);
            Connection itunesConnection = connectionMock(itunesResponse);
            Connection imageConnection = connectionMock(imageResponse);
            jsoup.when(() -> Jsoup.connect(anyString())).thenAnswer(invocation -> {
                String url = invocation.getArgument(0);
                return url.startsWith("https://example.com") ? imageConnection : itunesConnection;
            });

            MusicAlbumCoverSearcher searcher = new MusicAlbumCoverSearcher(DEFAULT_PROPS);

            CoverImage cover = searcher.findCover("Artista Album");
            assertNotNull(cover);
            assertEquals("image/jpeg", cover.contentType());
        }
    }
}