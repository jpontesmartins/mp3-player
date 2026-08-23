package com.mp3player.lyrics.infrastructure;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Testes do {@link JsoupLyricsScraper} sem nenhum acesso à rede: o
 * {@code Jsoup.connect} é mockado estaticamente e roteia cada URL para um status
 * e um documento simulados (a página da letra, a página de busca ou a do artista).
 * Os tweaks de string (slug, "sem The", inversão do artista) continuam sendo o
 * código real.
 */
class JsoupLyricsScraperTest {

    private static final Document EMPTY = new Document("");
    private static final LyricsProperties DEFAULT_PROPS = new LyricsProperties(
            "https://www.letras.mus.br", "Mozilla/5.0", "/", 8000, 15000);

    private static String lyricHtml() {
        return "<html><body><div class=\"lyric-original\"><p>linha um</p><p>linha dois</p></div></body></html>";
    }

    private static String lyricText() {
        return "linha um\nlinha dois";
    }

    /**
     * Instala o mock estático do Jsoup: {@code connect} devolve uma conexão cujo
     * {@code get()} entrega o documento mapeado para a URL e cujo {@code execute()}
     * devolve o status mapeado (404 por padrão). {@code Jsoup.parse} segue real.
     */
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
    void fetchReturnsLyricsFromDirectUrl() throws IOException {
        String artistPageUrl = "https://www.letras.mus.br/legião-urbana/";
        String directUrl = "https://www.letras.mus.br/legião-urbana/tempo-perdido/";
        Map<String, Document> docs = new HashMap<>();
        docs.put(artistPageUrl, Jsoup.parse("<html><body></body></html>"));
        docs.put(directUrl, Jsoup.parse(lyricHtml()));
        Map<String, Integer> statuses = new HashMap<>();
        statuses.put(directUrl, 200);

        try (MockedStatic<Jsoup> jsoup = mockJsoup(docs, statuses)) {
            String text = new JsoupLyricsScraper(DEFAULT_PROPS).fetch("Legião Urbana", "Tempo Perdido");
            assertEquals(lyricText(), text);
        }
    }

    @Test
    void fetchFallsBackToSearchAndStripsTranslationSuffix() throws IOException {
        String artistPageUrl = "https://www.letras.mus.br/legião-urbana/";
        String searchUrl = "https://www.letras.mus.br/?q="
                + java.net.URLEncoder.encode("Legião Urbana Tempo Perdido", java.nio.charset.StandardCharsets.UTF_8);
        String translatedUrl = "https://www.letras.mus.br/legião-urbana/tempo-perdido-traducao.html";
        String strippedUrl = translatedUrl.substring(0, translatedUrl.length() - "traducao.html".length());

        Document artistDoc = Jsoup.parse("<html><body></body></html>");
        Document searchDoc = Jsoup.parse("<html><body><a class=\"gs-title\" href=\"" + translatedUrl + "\">link</a></body></html>");
        Document lyricsDoc = Jsoup.parse(lyricHtml());

        Map<String, Document> docs = new HashMap<>();
        docs.put(artistPageUrl, artistDoc);
        docs.put(searchUrl, searchDoc);
        docs.put(strippedUrl, lyricsDoc);
        Map<String, Integer> statuses = new HashMap<>();

        try (MockedStatic<Jsoup> jsoup = mockJsoup(docs, statuses)) {
            String text = new JsoupLyricsScraper(DEFAULT_PROPS).fetch("Legião Urbana", "Tempo Perdido");

            var captor = org.mockito.ArgumentCaptor.forClass(String.class);
            jsoup.verify(() -> Jsoup.connect(captor.capture()), org.mockito.Mockito.atLeastOnce());
            assertTrue(captor.getAllValues().contains(strippedUrl),
                    "deveria acessar " + strippedUrl + " mas acessou: " + captor.getAllValues());

            assertEquals(lyricText(), text);
        }
    }

    @Test
    void fetchReturnsNotFoundMessageWhenNothingFound() throws IOException {
        try (MockedStatic<Jsoup> jsoup = mockJsoup(new HashMap<>(), new HashMap<>())) {
            String text = new JsoupLyricsScraper(DEFAULT_PROPS).fetch("Artista", "Musica");
            assertTrue(text.contains("Musica"));
            assertTrue(text.contains("Artista"));
            assertTrue(text.startsWith("Letra não encontrada"));
        }
    }

    @Test
    void fetchHandlesIOExceptionFromAllCandidates() throws IOException {
        String directUrl = "https://www.letras.mus.br/artista/musica/";
        Map<String, Document> docs = new HashMap<>();
        docs.put(directUrl, EMPTY);
        Map<String, Integer> statuses = new HashMap<>();
        statuses.put(directUrl, 200);

        MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class);
        jsoup.when(() -> Jsoup.parse(anyString())).thenCallRealMethod();
        jsoup.when(() -> Jsoup.connect(anyString())).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            Connection connection = mock(Connection.class, RETURNS_SELF);
            if (url.contains("?q=")) {
                Document emptySearch = new Document("");
                when(connection.get()).thenReturn(emptySearch);
                when(connection.execute()).thenReturn(mock(Connection.Response.class));
            } else {
                Connection.Response errorResponse = mock(Connection.Response.class);
                when(errorResponse.statusCode()).thenReturn(404);
                when(connection.execute()).thenReturn(errorResponse);
                when(connection.get()).thenThrow(new IOException("rede"));
            }
            return connection;
        });

        try {
            String text = new JsoupLyricsScraper(DEFAULT_PROPS).fetch("Artista", "Musica");
            assertTrue(text.startsWith("Letra não encontrada"));
            assertTrue(text.contains("Artista"));
        } finally {
            jsoup.close();
        }
    }

    @Test
    void artistPageFindsExactMatch() throws IOException {
        String artistPageUrl = "https://www.letras.mus.br/james-blunt/";
        String lyricsUrl = "https://www.letras.mus.br/james-blunt/no-tears/";
        String artistPageHtml = "<html><body>"
                + "<a href=\"/james-blunt/tears-and-rain/\">Tears And Rain</a>"
                + "<a href=\"/james-blunt/no-tears/\">No Tears</a>"
                + "</body></html>";
        Document artistDoc = Jsoup.parse(artistPageHtml);
        Document lyricsDoc = Jsoup.parse(lyricHtml());

        Map<String, Document> docs = new HashMap<>();
        docs.put(artistPageUrl, artistDoc);
        docs.put(lyricsUrl, lyricsDoc);
        Map<String, Integer> statuses = new HashMap<>();
        statuses.put(lyricsUrl, 200);

        try (MockedStatic<Jsoup> jsoup = mockJsoup(docs, statuses)) {
            String text = new JsoupLyricsScraper(DEFAULT_PROPS).fetch("James Blunt", "No Tears");
            assertEquals(lyricText(), text);
        }
    }

    @Test
    void artistPageRejectsPartialMatch() throws IOException {
        String artistPageUrl = "https://www.letras.mus.br/radiohead/";
        String artistPageHtml = "<html><body>"
                + "<a href=\"/radiohead/you-and-whose-army/\">You And Whose Army</a>"
                + "<a href=\"/radiohead/just/\">Just</a>"
                + "</body></html>";
        Document artistDoc = Jsoup.parse(artistPageHtml);

        Map<String, Document> docs = new HashMap<>();
        docs.put(artistPageUrl, artistDoc);
        Map<String, Integer> statuses = new HashMap<>();

        try (MockedStatic<Jsoup> jsoup = mockJsoup(docs, statuses)) {
            String text = new JsoupLyricsScraper(DEFAULT_PROPS).fetch("Radiohead", "You");
            assertTrue(text.startsWith("Letra não encontrada"));
            assertTrue(text.contains("You"));
        }
    }

    @Test
    void searchDoesNotMatchPartialTitle() throws IOException {
        String searchUrl = "https://www.letras.mus.br/?q="
                + java.net.URLEncoder.encode("Radiohead You", java.nio.charset.StandardCharsets.UTF_8);
        String searchHtml = "<html><body>"
                + "<a class=\"gs-title\" href=\"https://www.letras.mus.br/radiohead/you-and-whose-army/\">You And Whose Army</a>"
                + "<a class=\"gs-title\" href=\"https://www.letras.mus.br/radiohead/you/\">You</a>"
                + "</body></html>";
        Document searchDoc = Jsoup.parse(searchHtml);
        Document lyricsDoc = Jsoup.parse(lyricHtml());

        Map<String, Document> docs = new HashMap<>();
        docs.put(searchUrl, searchDoc);
        docs.put("https://www.letras.mus.br/radiohead/you/", lyricsDoc);
        Map<String, Integer> statuses = new HashMap<>();
        statuses.put("https://www.letras.mus.br/radiohead/you/", 200);

        try (MockedStatic<Jsoup> jsoup = mockJsoup(docs, statuses)) {
            String text = new JsoupLyricsScraper(DEFAULT_PROPS).fetch("Radiohead", "You");
            assertEquals(lyricText(), text);
        }
    }
}
