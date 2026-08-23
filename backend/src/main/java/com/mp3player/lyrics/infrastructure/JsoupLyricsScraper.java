package com.mp3player.lyrics.infrastructure;

import com.mp3player.lyrics.infrastructure.LyricsProperties;
import com.mp3player.lyrics.domain.port.LyricsScraper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Scraper de letras que busca em www.letras.mus.br.
 * Prioridade: (1) página do artista com título exato, (2) URL direta,
 * (3) busca via Google Custom Search.
 */
@Component
public class JsoupLyricsScraper implements LyricsScraper {

    private static final Logger log = LoggerFactory.getLogger(JsoupLyricsScraper.class);
    private final LyricsProperties props;

    public JsoupLyricsScraper(LyricsProperties props) {
        this.props = props;
    }

    @Override
    public String fetch(String artist, String title) throws IOException {
        List<String> candidates = new ArrayList<>();

        String artistPage = tryArtistPage(artist, title);
        if (artistPage != null) candidates.add(artistPage);

        String direct = tryDirectUrl(artist, title);
        if (direct != null) candidates.add(direct);

        String search = searchForUrl(artist, title);
        if (search != null) candidates.add(search);

        for (String candidate : candidates) {
            String page = stripTranslation(candidate);
            log.info("[Scraper] Buscando página de letra: {}", page);
            try {
                Document lyricDoc = Jsoup.connect(page)
                        .userAgent(props.userAgent())
                        .referrer(props.baseUrl())
                        .timeout(props.timeoutFetch())
                        .get();

                Element lyricDiv = lyricDoc.selectFirst("div.lyric-original");
                if (lyricDiv == null) {
                    log.warn("[Scraper] div.lyric-original não encontrada em {}", page);
                    continue;
                }

                for (Element p : lyricDiv.select("p")) {
                    p.after("<br>");
                }
                return lyricDiv.wholeText().trim();
            } catch (IOException e) {
                log.warn("[Scraper] Falha ao buscar página {}", page, e);
            }
        }

        log.warn("[Scraper] Nenhuma URL encontrada para \"{}\" - \"{}\"", artist, title);
        return "Letra não encontrada para \"" + title + "\" de " + artist;
    }

    /** Remove o sufixo "traducao.html" quando presente. */
    private static String stripTranslation(String href) {
        if (href != null && href.endsWith("traducao.html")) {
            return href.substring(0, href.length() - "traducao.html".length());
        }
        return href;
    }

    /**
     * Busca na página do artista. Tenta múltiplos slugs do artista (direto,
     * invertido, sem "the") e procura um link cujo texto corresponda exatamente
     * ao título da música (após slugificação).
     */
    private String tryArtistPage(String artist, String title) throws IOException {
        if (title.isEmpty()) return null;
        String titleSlug = toSlug(title);

        List<String> artistSlugs = new ArrayList<>();
        addSlug(artistSlugs, toSlug(artist));
        addSlug(artistSlugs, toSlug(withoutThe(artist)));
        addSlug(artistSlugs, invertedArtistSlug(artist));
        addSlug(artistSlugs, invertedArtistSlug(withoutThe(artist)));

        for (String artistSlug : artistSlugs) {
            String found = searchArtistPage(artistSlug, titleSlug);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Acessa a página do artista e procura um link cujo texto, após
     * slugificação, corresponda exatamente ao título.
     */
    private String searchArtistPage(String artistSlug, String titleSlug) throws IOException {
        String pageUrl = props.baseUrl() + "/" + artistSlug + "/";
        log.info("[Scraper] Buscando página do artista: {}", pageUrl);

        Document artistDoc;
        try {
            artistDoc = Jsoup.connect(pageUrl)
                    .userAgent(props.userAgent())
                    .referrer(props.baseUrl())
                    .timeout(props.timeoutFetch())
                    .ignoreHttpErrors(true)
                    .get();
        } catch (IOException e) {
            log.warn("[Scraper] Falha ao buscar página do artista {}", pageUrl, e);
            return null;
        }

        // Busca exata: slug do link == slug do título
        Element link = artistDoc.select("a[href]").stream()
                .filter(a -> {
                    String href = a.attr("href");
                    return href.contains("/" + artistSlug + "/");
                })
                .filter(a -> toSlug(a.text()).equals(titleSlug))
                .findFirst()
                .orElse(null);

        if (link == null) {
            log.warn("[Scraper] Nenhum link exato para \"{}\" na página {}", titleSlug, pageUrl);
            return null;
        }

        String href = link.absUrl("href");
        if (href.isBlank()) href = link.attr("href");
        if (!href.startsWith("http")) {
            href = props.baseUrl() + (href.startsWith("/") ? "" : "/") + href;
        }
        log.info("[Scraper] Encontrado: {} -> {}", link.text(), href);
        return href;
    }

    private String tryDirectUrl(String artist, String title) {
        if (artist.isEmpty() || title.isEmpty()) return null;
        String titleSlug = toSlug(title);
        List<String> slugs = new ArrayList<>();
        slugs.add(toSlug(artist));
        String artistNoThe = toSlug(withoutThe(artist));
        if (!artistNoThe.isEmpty() && !artistNoThe.equals(slugs.get(0))) {
            slugs.add(artistNoThe);
        }

        for (String artistSlug : slugs) {
            String url = props.baseUrl() + "/" + artistSlug + "/" + titleSlug + "/";
            log.info("[Scraper] Tentando URL direta: {}", url);
            try {
                int status = Jsoup.connect(url)
                        .userAgent(props.userAgent())
                        .timeout(props.timeoutConnect())
                        .execute().statusCode();
                if (status == 200) return url;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String searchForUrl(String artist, String title) throws IOException {
        String query = java.net.URLEncoder.encode(artist + " " + title, StandardCharsets.UTF_8);
        String searchUrl = props.baseUrl() + props.searchPath() + "?q=" + query;
        log.info("[Scraper] Buscando: {}", searchUrl);

        Document searchDoc = Jsoup.connect(searchUrl)
                .userAgent(props.userAgent())
                .referrer(props.baseUrl())
                .timeout(props.timeoutFetch())
                .get();

        String href = null;

        Element gsLink = searchDoc.selectFirst("a.gs-title");
        if (gsLink != null) href = gsLink.attr("href");

        if (href == null || href.isEmpty()) {
            Element divLink = searchDoc.selectFirst(".gs-title a");
            if (divLink != null) href = divLink.attr("href");
        }

        if (href == null || href.isEmpty()) {
            Element anyLink = searchDoc.select("a[href*='letras.mus.br']").stream()
                    .filter(a -> !a.attr("href").contains("?"))
                    .findFirst().orElse(null);
            if (anyLink != null) href = anyLink.attr("href");
        }

        if (href == null || href.isEmpty()) {
            String titleSlug = toSlug(title);
            Element matchingLink = searchDoc.select("a[href*='letras.mus.br']").stream()
                    .filter(a -> toSlug(a.text()).equals(titleSlug))
                    .findFirst().orElse(null);
            if (matchingLink != null) href = matchingLink.attr("href");
        }

        if (href == null || href.isEmpty()) {
            return null;
        }

        if (!href.startsWith("http")) {
            href = props.baseUrl() + (href.startsWith("/") ? "" : "/") + href;
        }
        if ((props.baseUrl() + "/").equals(href)) {
            return null;
        }
        return href;
    }

    /** Adiciona o slug à lista, ignorando vazios e duplicados. */
    private static void addSlug(List<String> slugs, String slug) {
        if (slug != null && !slug.isEmpty() && !slugs.contains(slug)) {
            slugs.add(slug);
        }
    }

    /** Remove o "The " inicial (case-insensitive) do nome do artista. */
    private static String withoutThe(String artist) {
        if (artist == null) return "";
        String trimmed = artist.trim();
        if (trimmed.toLowerCase().startsWith("the ")) {
            return trimmed.substring(4).trim();
        }
        return trimmed;
    }

    /** Inverte a ordem das palavras do artista e gera o slug (ex.: "joni mitchell" -> "mitchell-joni"). */
    private static String invertedArtistSlug(String artist) {
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

    private static String toSlug(String s) {
        return s.toLowerCase()
                .replaceAll("[^a-z0-9áéíóúãõâêîôûçñ\\s]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }
}
