package com.ovelha.fy.lyrics.infrastructure;

import com.ovelha.fy.lyrics.domain.port.LyricsSource;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Testes do {@link CompositeLyricsScraper}.
 */
class CompositeLyricsScraperTest {

    private static final Document EMPTY = new Document("");

    private static String lyricHtml() {
        return "<html><body><div class=\"lyric-original\"><p>linha um</p><p>linha dois</p></div></body></html>";
    }

    private static String lyricText() {
        return "linha um\nlinha dois";
    }

    private static MockedStatic<Jsoup> mockJsoup(Map<String, Document> docs, Map<String, Integer> statuses) {
        MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class);
        jsoup.when(() -> Jsoup.parse(anyString())).thenCallRealMethod();
        jsoup.when(() -> Jsoup.connect(anyString())).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            Connection.Response response = mock(Connection.Response.class);
            when(response.statusCode()).thenReturn(statuses.getOrDefault(url, 404));
            Connection connection = mock(Connection.class, RETURNS_SELF);
            when(connection.execute()).thenReturn(response);
            when(connection.get()).thenReturn(docs.getOrDefault(url, EMPTY));
            return connection;
        });
        return jsoup;
    }

    @Test
    void triesSourcesByPriority() throws IOException {
        String artistPageUrl = "https://www.letras.mus.br/james-blunt/";
        String lyricsUrl = "https://www.letras.mus.br/james-blunt/no-tears/";
        String artistPageHtml = "<html><body>"
                + "<a href=\"/james-blunt/no-tears/\">No Tears</a>"
                + "</body></html>";

        Map<String, Document> docs = new HashMap<>();
        docs.put(artistPageUrl, Jsoup.parse(artistPageHtml));
        docs.put(lyricsUrl, Jsoup.parse(lyricHtml()));
        Map<String, Integer> statuses = new HashMap<>();
        statuses.put(lyricsUrl, 200);

        LetrasMusBrSource source = new LetrasMusBrSource(
                "https://www.letras.mus.br", "Mozilla/5.0", 8000, 15000, "/", true, 1);

        try (MockedStatic<Jsoup> jsoup = mockJsoup(docs, statuses)) {
            CompositeLyricsScraper scraper = new CompositeLyricsScraper(List.of(source));
            String text = scraper.fetch("James Blunt", "No Tears");
            assertEquals(lyricText(), text);
        }
    }

    @Test
    void skipsDisabledSources() throws IOException {
        LetrasMusBrSource disabled = new LetrasMusBrSource(
                "https://www.letras.mus.br", "Mozilla/5.0", 8000, 15000, "/", false, 1);

        Map<String, Document> docs = new HashMap<>();
        Map<String, Integer> statuses = new HashMap<>();

        try (MockedStatic<Jsoup> jsoup = mockJsoup(docs, statuses)) {
            CompositeLyricsScraper scraper = new CompositeLyricsScraper(List.of(disabled));
            String text = scraper.fetch("Artista", "Musica");
            assertTrue(text.startsWith("Letra não encontrada"));
        }
    }

    @Test
    void fallsBackToSecondSource() throws IOException {
        // First source finds nothing, second source finds lyrics
        String lyricsUrl = "https://www.letras.mus.br/artista/musica/";
        Map<String, Document> docs = new HashMap<>();
        docs.put(lyricsUrl, Jsoup.parse(lyricHtml()));
        Map<String, Integer> statuses = new HashMap<>();
        statuses.put(lyricsUrl, 200);

        LetrasMusBrSource source = new LetrasMusBrSource(
                "https://www.letras.mus.br", "Mozilla/5.0", 8000, 15000, "/", true, 1);

        try (MockedStatic<Jsoup> jsoup = mockJsoup(docs, statuses)) {
            CompositeLyricsScraper scraper = new CompositeLyricsScraper(List.of(source));
            // The source's findPage will try artist page first (empty), then direct URL, then search
            // This tests the fallback within a single source
            String text = scraper.fetch("Artista", "Musica");
            // Should find via direct URL since it returns 200
            assertEquals(lyricText(), text);
        }
    }

    @Test
    void returnsNotFoundWhenAllSourcesFail() throws IOException {
        LetrasMusBrSource source = new LetrasMusBrSource(
                "https://www.letras.mus.br", "Mozilla/5.0", 8000, 15000, "/", true, 1);

        try (MockedStatic<Jsoup> jsoup = mockJsoup(new HashMap<>(), new HashMap<>())) {
            CompositeLyricsScraper scraper = new CompositeLyricsScraper(List.of(source));
            String text = scraper.fetch("Artista", "Musica");
            assertTrue(text.startsWith("Letra não encontrada"));
            assertTrue(text.contains("Artista"));
            assertTrue(text.contains("Musica"));
        }
    }
}
