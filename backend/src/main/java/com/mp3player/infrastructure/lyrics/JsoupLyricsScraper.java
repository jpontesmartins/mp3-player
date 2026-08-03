package com.mp3player.infrastructure.lyrics;

import com.mp3player.domain.port.LyricsScraper;
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
 * Scraper de letras que busca em www.letras.mus.br: URL direta primeiro, depois
 * busca pela música, e por fim tenta a página do artista com o nome invertido
 * (ex.: "Joni Mitchell" -> /mitchell-joni/), localizando a música pelo título.
 */
@Component
public class JsoupLyricsScraper implements LyricsScraper {

    private static final Logger log = LoggerFactory.getLogger(JsoupLyricsScraper.class);
    private static final String USER_AGENT = "Mozilla/5.0";

    @Override
    public String fetch(String artist, String title) throws IOException {
        List<String> candidates = new ArrayList<>();
        String direct = tryDirectUrl(artist, title);
        if (direct != null) candidates.add(direct);
        String search = searchForUrl(artist, title);
        if (search != null) candidates.add(search);
        String artistPage = tryArtistPage(artist, title);
        if (artistPage != null) candidates.add(artistPage);

        for (String candidate : candidates) {
            String page = stripTranslation(candidate);
            log.info("Fetching lyrics page: {}", page);
            try {
                Document lyricDoc = Jsoup.connect(page)
                        .userAgent(USER_AGENT)
                        .referrer("https://www.letras.mus.br")
                        .timeout(15000)
                        .get();

                Element lyricDiv = lyricDoc.selectFirst("div.lyric-original");
                if (lyricDiv == null) {
                    log.warn("div.lyric-original not found on {}", page);
                    continue;
                }

                for (Element p : lyricDiv.select("p")) {
                    p.after("<br>");
                }
                return lyricDiv.wholeText().trim();
            } catch (IOException e) {
                log.warn("Failed to fetch page {}", page, e);
            }
        }

        log.warn("No URL found for \"{}\" - \"{}\"", artist, title);
        return "Letra não encontrada para \"" + title + "\" de " + artist;
    }

    /** Remove o sufixo "traducao.html" quando presente. */
    private static String stripTranslation(String href) {
        if (href != null && href.endsWith("traducao.html")) {
            return href.substring(0, href.length() - "traducao.html".length());
        }
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
            String url = "https://www.letras.mus.br/" + artistSlug + "/" + titleSlug + "/";
            log.info("Trying direct URL: {}", url);
            try {
                int status = Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .timeout(8000)
                        .execute().statusCode();
                if (status == 200) return url;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String searchForUrl(String artist, String title) throws IOException {
        String query = java.net.URLEncoder.encode(artist + " " + title, StandardCharsets.UTF_8);
        String searchUrl = "https://www.letras.mus.br/?q=" + query;
        log.info("Searching: {}", searchUrl);

        Document searchDoc = Jsoup.connect(searchUrl)
                .userAgent(USER_AGENT)
                .referrer("https://www.letras.mus.br")
                .timeout(15000)
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
            String lowerTitle = title.toLowerCase();
            Element matchingLink = searchDoc.select("a[href*='letras.mus.br']").stream()
                    .filter(a -> a.text().toLowerCase().contains(lowerTitle))
                    .findFirst().orElse(null);
            if (matchingLink != null) href = matchingLink.attr("href");
        }

        if (href == null || href.isEmpty()) {
            return null;
        }

        if (!href.startsWith("http")) {
            href = "https://www.letras.mus.br" + (href.startsWith("/") ? "" : "/") + href;
        }
        if ("https://www.letras.mus.br/".equals(href)) {
            return null;
        }
        return href;
    }

    /**
     * Tenta a página do artista com o nome invertido (ex.: "Joni Mitchell" ->
     * /mitchell-joni/). Primeiro tenta a URL direta da música nessa página; se
     * falhar, acessa a página do artista e procura um link cujo texto contenha
     * o título da música.
     */
    private String tryArtistPage(String artist, String title) throws IOException {
        if (title.isEmpty()) return null;
        List<String> slugs = new ArrayList<>();
        addSlug(slugs, invertedArtistSlug(artist));
        addSlug(slugs, invertedArtistSlug(withoutThe(artist)));
        addSlug(slugs, toSlug(artist));
        addSlug(slugs, toSlug(withoutThe(artist)));

        for (String slug : slugs) {
            String href = tryArtistSlug(slug, title);
            if (href != null) return href;
        }
        return null;
    }

    /** Adiciona o slug à lista, ignorando vazios e duplicados. */
    private static void addSlug(List<String> slugs, String slug) {
        if (slug != null && !slug.isEmpty() && !slugs.contains(slug)) {
            slugs.add(slug);
        }
    }

    /**
     * Dado o slug da página do artista (ex.: "velvet-underground"), tenta a URL
     * direta da música e, se falhar, procura o link pelo título na página do artista.
     */
    private String tryArtistSlug(String inverted, String title) throws IOException {
        String direct = "https://www.letras.mus.br/" + inverted + "/" + toSlug(title) + "/";
        log.info("Trying inverted artist direct URL: {}", direct);
        try {
            int status = Jsoup.connect(direct)
                    .userAgent(USER_AGENT)
                    .timeout(8000)
                    .execute().statusCode();
            if (status == 200) return direct;
        } catch (Exception ignored) {
        }

        String pageUrl = "https://www.letras.mus.br/" + inverted + "/";
        log.info("Fetching artist page: {}", pageUrl);
        Document artistDoc;
        try {
            artistDoc = Jsoup.connect(pageUrl)
                    .userAgent(USER_AGENT)
                    .referrer("https://www.letras.mus.br")
                    .timeout(15000)
                    .ignoreHttpErrors(true)
                    .get();
        } catch (IOException e) {
            log.warn("Failed to fetch artist page {}", pageUrl, e);
            return null;
        }

        String lowerTitle = title.toLowerCase();
        Element link = artistDoc.select("a[href*='" + inverted + "']").stream()
                .filter(a -> a.text().toLowerCase().contains(lowerTitle))
                .findFirst().orElse(null);
        if (link == null) {
            link = artistDoc.select("a[href*='letras.mus.br']").stream()
                    .filter(a -> a.text().toLowerCase().contains(lowerTitle))
                    .findFirst().orElse(null);
        }
        if (link == null) {
            log.warn("No song link found on artist page for \"{}\"", title);
            return null;
        }

        String page = link.absUrl("href");
        if (page.isBlank()) page = link.attr("href");
        if (!page.startsWith("http")) {
            page = "https://www.letras.mus.br" + (page.startsWith("/") ? "" : "/") + page;
        }
        return page;
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

    private static String lowerTitle(String s) {
        return s.toLowerCase();
    }
}