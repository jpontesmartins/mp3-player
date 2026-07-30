package com.mp3player.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class LyricsService {

    private static final Logger log = LoggerFactory.getLogger(LyricsService.class);

    private final Mp3PlayService mp3PlayService;

    public LyricsService(Mp3PlayService mp3PlayService) {
        this.mp3PlayService = mp3PlayService;
    }

    public String getCachedLyrics(String filePath) throws IOException {
        Path txtFile = resolveTxtFile(filePath);
        if (txtFile != null && Files.exists(txtFile)) {
            log.info("📜 Cache hit: {}", txtFile.getFileName());
            return Files.readString(txtFile, StandardCharsets.UTF_8);
        }
        log.info("📜 Cache miss");
        return null;
    }

    public String getLyrics(String filePath) throws IOException {
        Path txtFile = resolveTxtFile(filePath);
        if (txtFile != null && Files.exists(txtFile)) {
            log.info("📜 Lyrics from cache: {}", txtFile.getFileName());
            return Files.readString(txtFile, StandardCharsets.UTF_8);
        }

        String artist = extractArtist(filePath);
        String title = extractTitle(filePath);
        log.info("📜 Scraping lyrics for \"{}\" - \"{}\"", artist, title);

        String lyrics = fetchFromWeb(artist, title);

        if (txtFile != null) {
            Files.writeString(txtFile, lyrics, StandardCharsets.UTF_8);
            log.info("📜 Saved to {}", txtFile.getFileName());
        }

        return lyrics;
    }

    private String fetchFromWeb(String artist, String title) throws IOException {
        String href = tryDirectUrl(artist, title);
        if (href != null) {
            log.info("📜 Direct URL OK: {}", href);
        } else {
            log.info("📜 Direct URL failed, searching...");
            href = searchForUrl(artist, title);
        }
        if (href == null) {
            log.warn("📜 No URL found for \"{}\" - \"{}\"", artist, title);
            return "Letra não encontrada para \"" + title + "\" de " + artist;
        }

        if (href.endsWith("traducao.html")) {
            log.info("📜 Removing traducao.html suffix");
            href = href.substring(0, href.length() - "traducao.html".length());
        }

        log.info("📜 Fetching lyrics page: {}", href);
        Document lyricDoc = Jsoup.connect(href)
                .userAgent("Mozilla/5.0")
                .referrer("https://www.letras.mus.br")
                .timeout(15000)
                .get();

        Element lyricDiv = lyricDoc.selectFirst("div.lyric-original");
        if (lyricDiv == null) {
            log.warn("📜 div.lyric-original not found on page");
            return "Letra não encontrada para \"" + title + "\" de " + artist;
        }

        log.info("📜 Lyrics extracted successfully");
        for (Element p : lyricDiv.select("p")) {
            p.after("<br>");
        }
        return lyricDiv.wholeText().trim();
    }

    private String tryDirectUrl(String artist, String title) {
        if (artist.isEmpty() || title.isEmpty()) return null;
        String url = "https://www.letras.mus.br/" + toSlug(artist) + "/" + toSlug(title) + "/";
        log.info("📜 Trying direct URL: {}", url);
        try {
            int status = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(8000)
                    .execute().statusCode();
            log.info("📜 Direct URL status: {}", status);
            if (status == 200) return url;
        } catch (Exception ignored) {
            log.info("📜 Direct URL failed with exception");
        }
        return null;
    }

    private String searchForUrl(String artist, String title) throws IOException {
        String query = java.net.URLEncoder.encode(artist + " " + title, StandardCharsets.UTF_8);
        String searchUrl = "https://www.letras.mus.br/?q=" + query;
        log.info("📜 Searching: {}", searchUrl);

        Document searchDoc = Jsoup.connect(searchUrl)
                .userAgent("Mozilla/5.0")
                .referrer("https://www.letras.mus.br")
                .timeout(15000)
                .get();

        String href = null;

        Element gsLink = searchDoc.selectFirst("a.gs-title");
        if (gsLink != null) {
            href = gsLink.attr("href");
            log.info("📜 Found via a.gs-title: {}", href);
        }

        if (href == null || href.isEmpty()) {
            Element divLink = searchDoc.selectFirst(".gs-title a");
            if (divLink != null) {
                href = divLink.attr("href");
                log.info("📜 Found via .gs-title a: {}", href);
            }
        }

        if (href == null || href.isEmpty()) {
            Element anyLink = searchDoc.select("a[href*='letras.mus.br']").stream()
                .filter(a -> !a.attr("href").contains("?"))
                .findFirst().orElse(null);
            if (anyLink != null) {
                href = anyLink.attr("href");
                log.info("📜 Found via generic link: {}", href);
            }
        }

        if (href == null || href.isEmpty()) {
            String lowerTitle = title.toLowerCase();
            Element matchingLink = searchDoc.select("a[href*='letras.mus.br']").stream()
                .filter(a -> a.text().toLowerCase().contains(lowerTitle))
                .findFirst().orElse(null);
            if (matchingLink != null) {
                href = matchingLink.attr("href");
                log.info("📜 Found via title match: {}", href);
            }
        }

        if (href == null || href.isEmpty()) {
            log.warn("📜 No link found on search page");
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

    private Path resolveTxtFile(String filePath) throws IOException {
        Map<String, String> id3 = mp3PlayService.getId3TagsForFile(filePath);
        String artist = sanitize(id3.getOrDefault("artist", ""));
        String title = sanitize(id3.getOrDefault("title", ""));
        if (artist.isEmpty() && title.isEmpty()) {
            String name = Paths.get(filePath).getFileName().toString();
            if (name.toLowerCase().endsWith(".mp3")) {
                name = name.substring(0, name.length() - 4);
            }
            int dash = name.indexOf(" - ");
            if (dash > 0) {
                artist = sanitize(name.substring(0, dash));
                title = sanitize(name.substring(dash + 3));
            } else {
                title = sanitize(name);
            }
        }

        Path parent = Paths.get(filePath).getParent();
        if (parent == null) {
            throw new IOException("No parent directory");
        }

        String fileName = (artist.isEmpty() ? "" : artist + " - ") + title + ".txt";
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        return parent.resolve(fileName);
    }

    private String extractArtist(String filePath) {
        try {
            Map<String, String> id3 = mp3PlayService.getId3TagsForFile(filePath);
            String artist = sanitize(id3.getOrDefault("artist", ""));
            if (!artist.isEmpty()) return artist;
        } catch (Exception ignored) {}
        String name = Paths.get(filePath).getFileName().toString();
        if (name.toLowerCase().endsWith(".mp3")) name = name.substring(0, name.length() - 4);
        int dash = name.indexOf(" - ");
        return dash > 0 ? sanitize(name.substring(0, dash)) : "";
    }

    private String extractTitle(String filePath) {
        try {
            Map<String, String> id3 = mp3PlayService.getId3TagsForFile(filePath);
            String title = sanitize(id3.getOrDefault("title", ""));
            if (!title.isEmpty()) return title;
        } catch (Exception ignored) {}
        String name = Paths.get(filePath).getFileName().toString();
        if (name.toLowerCase().endsWith(".mp3")) name = name.substring(0, name.length() - 4);
        int dash = name.indexOf(" - ");
        return dash > 0 ? sanitize(name.substring(dash + 3)) : sanitize(name);
    }

    private static String sanitize(String s) {
        return s == null ? "" : s.trim();
    }
}
