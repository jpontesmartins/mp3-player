package com.mp3player.lyrics.infrastructure;

import com.mp3player.lyrics.domain.port.LyricsSource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Template method para fontes de letras. Fornece o fluxo comum
 * (buscar URL → carregar página → extrair letra) e utilitários
 * compartilhados (slug, "sem The", inversão de artista).
 *
 * <p>Subclasses implementam os métodos abstratos que variam por site.</p>
 */
public abstract class AbstractLyricsSource implements LyricsSource {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final String baseUrl;
    private final String userAgent;
    private final int timeoutConnect;
    private final int timeoutFetch;

    protected AbstractLyricsSource(String baseUrl, String userAgent,
                                   int timeoutConnect, int timeoutFetch) {
        this.baseUrl = baseUrl;
        this.userAgent = userAgent;
        this.timeoutConnect = timeoutConnect;
        this.timeoutFetch = timeoutFetch;
    }

    // ── Getters para subclasses ──────────────────────────────────────

    protected String getBaseUrl() { return baseUrl; }
    protected String getUserAgent() { return userAgent; }
    protected int getTimeoutConnect() { return timeoutConnect; }
    protected int getTimeoutFetch() { return timeoutFetch; }

    // ── Template method ──────────────────────────────────────────────

    /**
     * Fluxo principal: busca a URL da letra e extrai o texto.
     * Pode ser sobrescrito por subclasses que precisem de lógica diferente.
     */
    public String fetchFromSource(String artist, String title) throws IOException {
        String pageUrl = findPage(artist, title);
        if (pageUrl == null) return null;

        log.info("[{}] Buscando página de letra: {}", getName(), pageUrl);
        Document doc = connect(pageUrl);
        return extractLyrics(doc);
    }

    // ── HTTP helpers ─────────────────────────────────────────────────

    /** Faz GET na URL e retorna o Document. Lança IOException em caso de erro de rede. */
    protected Document connect(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(userAgent)
                .referrer(baseUrl)
                .timeout(timeoutFetch)
                .get();
    }

    /** Faz HEAD/GET e retorna o status code. */
    protected int statusCode(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(userAgent)
                    .timeout(timeoutConnect)
                    .execute().statusCode();
        } catch (Exception e) {
            return -1;
        }
    }

    /** Monta URL absoluta a partir de um path relativo. */
    protected String resolveUrl(String path) {
        if (path == null) return null;
        if (path.startsWith("http")) return path;
        return baseUrl + (path.startsWith("/") ? "" : "/") + path;
    }

    // ── Slug / string utilities ──────────────────────────────────────

    protected static String toSlug(String s) {
        return s.toLowerCase()
                .replaceAll("[^a-z0-9áéíóúãõâêîôûçñ\\s]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }

    protected static String withoutThe(String artist) {
        if (artist == null) return "";
        String trimmed = artist.trim();
        if (trimmed.toLowerCase().startsWith("the ")) {
            return trimmed.substring(4).trim();
        }
        return trimmed;
    }

    protected static String invertedArtistSlug(String artist) {
        if (artist == null || artist.isBlank()) return "";
        String[] parts = artist.trim().split("\\s+");
        if (parts.length < 2) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = parts.length - 1; i >= 0; i--) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(parts[i]);
        }
        return toSlug(sb.toString());
    }

    protected static void addSlug(List<String> slugs, String slug) {
        if (slug != null && !slug.isEmpty() && !slugs.contains(slug)) {
            slugs.add(slug);
        }
    }

    /** Gera lista de slugs possíveis para o artista (direto, sem the, invertido). */
    protected List<String> artistSlugs(String artist) {
        List<String> slugs = new ArrayList<>();
        addSlug(slugs, toSlug(artist));
        addSlug(slugs, toSlug(withoutThe(artist)));
        addSlug(slugs, invertedArtistSlug(artist));
        addSlug(slugs, invertedArtistSlug(withoutThe(artist)));
        return slugs;
    }
}
