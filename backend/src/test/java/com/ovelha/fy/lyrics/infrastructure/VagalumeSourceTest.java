package com.ovelha.fy.lyrics.infrastructure;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Testes do {@link VagalumeSource} sem acesso à rede.
 */
class VagalumeSourceTest {

    private static VagalumeSource source() {
        return new VagalumeSource("https://www.vagalume.com.br", "Mozilla/5.0", 8000, 15000,
                "/search.php", true, 1);
    }

    @Test
    void searchFailureDoesNotPropagateAndSlugsAreAscii() throws IOException {
        List<String> requested = new ArrayList<>();
        try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
            jsoup.when(() -> Jsoup.connect(anyString())).thenAnswer(invocation -> {
                String url = invocation.getArgument(0);
                requested.add(url);
                Connection.Response response = mock(Connection.Response.class);
                when(response.statusCode()).thenReturn(404);
                Connection connection = mock(Connection.class, RETURNS_SELF);
                when(connection.execute()).thenReturn(response);
                when(connection.get()).thenThrow(new IOException("HTTP 404"));
                return connection;
            });

            assertNull(source().findPage("Nara Leão", "Miserere Nóbis"));
        }

        assertTrue(requested.contains("https://www.vagalume.com.br/nara-leao/miserere-nobis.html"));
        assertTrue(requested.stream().noneMatch(u -> u.contains("ã") || u.contains("ó")));
    }

    @Test
    void extractsLyricsFromLyricsDiv() {
        Document doc = Jsoup.parse("<html><body><div id=\"lyrics\"> linha </div></body></html>");
        assertEquals("linha", source().extractLyrics(doc));
    }
}
