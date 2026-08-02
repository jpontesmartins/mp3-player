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

/**
 * Lyrics scraper that fetches from www.letras.mus.br using a direct URL first
 * and falling back to a search for the song page.
 */
@Component
public class JsoupLyricsScraper implements LyricsScraper {

    private static final Logger log = LoggerFactory.getLogger(JsoupLyricsScraper.class);
    private static final String USER_AGENT = "Mozilla/5.0";

    @Override
    public String fetch(String artist, String title) throws IOException {
        String href = tryDirectUrl(artist, title);
        if (href == null) {
            log.info("Direct URL failed, searching...");
            href = searchForUrl(artist, title);
        }
        if (href == null) {
            log.warn("No URL found for \"{}\" - \"{}\"", artist, title);
            return "Letra não encontrada para \"" + title + "\" de " + artist;
        }

        if (href.endsWith("traducao.html")) {
            href = href.substring(0, href.length() - "traducao.html".length());
        }

        log.info("Fetching lyrics page: {}", href);
        Document lyricDoc = Jsoup.connect(href)
                .userAgent(USER_AGENT)
                .referrer("https://www.letras.mus.br")
                .timeout(15000)
                .get();

        Element lyricDiv = lyricDoc.selectFirst("div.lyric-original");
        if (lyricDiv == null) {
            log.warn("div.lyric-original not found on page");
            return "Letra não encontrada para \"" + title + "\" de " + artist;
        }

        for (Element p : lyricDiv.select("p")) {
            p.after("<br>");
        }
        return lyricDiv.wholeText().trim();
    }

    private String tryDirectUrl(String artist, String title) {
        if (artist.isEmpty() || title.isEmpty()) return null;
        String url = "https://www.letras.mus.br/" + toSlug(artist) + "/" + toSlug(title) + "/";
        log.info("Trying direct URL: {}", url);
        try {
            int status = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(8000)
                    .execute().statusCode();
            if (status == 200) return url;
        } catch (Exception ignored) {
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
        return href;
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