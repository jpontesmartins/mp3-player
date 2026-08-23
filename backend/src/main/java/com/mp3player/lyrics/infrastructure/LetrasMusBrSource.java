package com.mp3player.lyrics.infrastructure;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Fonte de letras para <a href="https://www.letras.mus.br">letras.mus.br</a>.
 *
 * <p>Estratégia de busca:</p>
 * <ol>
 *   <li>Página do artista — link com título exato</li>
 *   <li>URL direta — {@code /{artist-slug}/{title-slug}/}</li>
 *   <li>Busca Google Custom Search no site</li>
 * </ol>
 */
public class LetrasMusBrSource extends AbstractLyricsSource {

    private final boolean enabled;
    private final int priority;
    private final String searchPath;

    public LetrasMusBrSource(String baseUrl, String userAgent,
                             int timeoutConnect, int timeoutFetch,
                             String searchPath, boolean enabled, int priority) {
        super(baseUrl, userAgent, timeoutConnect, timeoutFetch);
        this.searchPath = searchPath;
        this.enabled = enabled;
        this.priority = priority;
    }

    @Override
    public String getName() { return "letras.mus.br"; }

    @Override
    public int getPriority() { return priority; }

    @Override
    public boolean isEnabled() { return enabled; }

    // ── findPage ─────────────────────────────────────────────────────

    @Override
    public String findPage(String artist, String title) throws IOException {
        String fromArtist = findOnArtistPage(artist, title);
        if (fromArtist != null) return fromArtist;

        String fromDirect = findDirectUrl(artist, title);
        if (fromDirect != null) return fromDirect;

        return findViaSearch(artist, title);
    }

    // ── extractLyrics ────────────────────────────────────────────────

    @Override
    public String extractLyrics(Document page) {
        Element lyricDiv = page.selectFirst("div.lyric-original");
        if (lyricDiv == null) return null;

        for (Element p : lyricDiv.select("p")) {
            p.after("<br>");
        }
        return lyricDiv.wholeText().trim();
    }

    // ── Estratégias de busca ─────────────────────────────────────────

    /**
     * Busca na página do artista por link com título exato.
     */
    private String findOnArtistPage(String artist, String title) throws IOException {
        String titleSlug = toSlug(title);

        for (String artistSlug : artistSlugs(artist)) {
            String pageUrl = getBaseUrl() + "/" + artistSlug + "/";
            log.info("[{}] Buscando página do artista: {}", getName(), pageUrl);

            Document artistDoc;
            try {
                artistDoc = connect(pageUrl);
            } catch (IOException e) {
                log.warn("[{}] Falha ao buscar página do artista {}", getName(), pageUrl, e);
                continue;
            }

            Element link = artistDoc.select("a[href]").stream()
                    .filter(a -> a.attr("href").contains("/" + artistSlug + "/"))
                    .filter(a -> toSlug(a.text()).equals(titleSlug))
                    .findFirst()
                    .orElse(null);

            if (link != null) {
                String href = resolveUrl(link.attr("href"));
                log.info("[{}] Encontrado: {} -> {}", getName(), link.text(), href);
                return href;
            }

            log.warn("[{}] Nenhum link exato para \"{}\" na página {}", getName(), titleSlug, pageUrl);
        }
        return null;
    }

    /**
     * Tenta URL direta: /{artist-slug}/{title-slug}/
     */
    private String findDirectUrl(String artist, String title) {
        if (artist.isEmpty() || title.isEmpty()) return null;
        String titleSlug = toSlug(title);

        for (String artistSlug : artistSlugs(artist)) {
            String url = getBaseUrl() + "/" + artistSlug + "/" + titleSlug + "/";
            log.info("[{}] Tentando URL direta: {}", getName(), url);
            if (statusCode(url) == 200) return url;
        }
        return null;
    }

    /**
     * Busca via Google Custom Search no site.
     */
    private String findViaSearch(String artist, String title) throws IOException {
        String query = java.net.URLEncoder.encode(artist + " " + title, StandardCharsets.UTF_8);
        String searchUrl = getBaseUrl() + searchPath + "?q=" + query;
        log.info("[{}] Buscando: {}", getName(), searchUrl);

        Document searchDoc = connect(searchUrl);
        String href = extractSearchResult(searchDoc, title);

        if (href == null || href.isEmpty()) return null;
        return resolveUrl(href);
    }

    /** Tenta extrair um link relevante dos resultados de busca. */
    private String extractSearchResult(Document doc, String title) {
        // 1. a.gs-title (Google Custom Search)
        Element gsLink = doc.selectFirst("a.gs-title");
        if (gsLink != null) return gsLink.attr("href");

        // 2. .gs-title a
        Element divLink = doc.selectFirst(".gs-title a");
        if (divLink != null) return divLink.attr("href");

        // 3. Qualquer link para letras.mus.br
        Element anyLink = doc.select("a[href*='letras.mus.br']").stream()
                .filter(a -> !a.attr("href").contains("?"))
                .findFirst().orElse(null);
        if (anyLink != null) return anyLink.attr("href");

        // 4. Link com título exato
        String titleSlug = toSlug(title);
        Element matchingLink = doc.select("a[href*='letras.mus.br']").stream()
                .filter(a -> toSlug(a.text()).equals(titleSlug))
                .findFirst().orElse(null);
        if (matchingLink != null) return matchingLink.attr("href");

        return null;
    }
}
