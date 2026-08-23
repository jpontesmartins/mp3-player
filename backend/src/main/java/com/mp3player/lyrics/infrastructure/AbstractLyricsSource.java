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

    /**
     * Construtor para subclasses de fontes de letras.
     *
     * @param baseUrl URL base do site da fonte
     * @param userAgent User-Agent utilizado nas requisições HTTP
     * @param timeoutConnect timeout de conexão em milissegundos
     * @param timeoutFetch timeout de busca em milissegundos
     */
    protected AbstractLyricsSource(String baseUrl, String userAgent,
                                   int timeoutConnect, int timeoutFetch) {
        this.baseUrl = baseUrl;
        this.userAgent = userAgent;
        this.timeoutConnect = timeoutConnect;
        this.timeoutFetch = timeoutFetch;
    }

    // ── Getters para subclasses ──────────────────────────────────────

    /**
     * Retorna a URL base do site da fonte.
     *
     * @return URL base
     */
    protected String getBaseUrl() { return baseUrl; }

    /**
     * Retorna o User-Agent utilizado nas requisições HTTP.
     *
     * @return User-Agent
     */
    protected String getUserAgent() { return userAgent; }

    /**
     * Retorna o timeout de conexão em milissegundos.
     *
     * @return timeout de conexão
     */
    protected int getTimeoutConnect() { return timeoutConnect; }

    /**
     * Retorna o timeout de busca em milissegundos.
     *
     * @return timeout de busca
     */
    protected int getTimeoutFetch() { return timeoutFetch; }

    // ── Template method ──────────────────────────────────────────────

    /**
     * Fluxo principal: busca a URL da letra e extrai o texto.
     * Pode ser sobrescrito por subclasses que precisem de lógica diferente.
     *
     * @param artist nome do artista
     * @param title título da música
     * @return texto da letra ou {@code null} se não encontrada
     * @throws IOException se ocorrer erro de rede
     */
    public String fetchFromSource(String artist, String title) throws IOException {
        String pageUrl = findPage(artist, title);
        if (pageUrl == null) return null;

        log.info("[{}] Buscando página de letra: {}", getName(), pageUrl);
        Document doc = connect(pageUrl);
        return extractLyrics(doc);
    }

    // ── HTTP helpers ─────────────────────────────────────────────────

    /**
     * Faz GET na URL e retorna o {@link Document} HTML.
     *
     * @param url URL a ser requisitada
     * @return documento HTML parseado
     * @throws IOException se ocorrer erro de rede ou parsing
     */
    protected Document connect(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(userAgent)
                .referrer(baseUrl)
                .timeout(timeoutFetch)
                .get();
    }

    /**
     * Faz HEAD/GET na URL e retorna o código de status HTTP.
     *
     * @param url URL a ser verificada
     * @return código de status HTTP, ou {@code -1} se ocorrer erro
     */
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

    /**
     * Monta URL absoluta a partir de um path relativo.
     *
     * @param path caminho relativo ou absoluto
     * @return URL absoluta, ou {@code null} se o path for nulo
     */
    protected String resolveUrl(String path) {
        if (path == null) return null;
        if (path.startsWith("http")) return path;
        return baseUrl + (path.startsWith("/") ? "" : "/") + path;
    }

    // ── Slug / string utilities ──────────────────────────────────────

    /**
     * Converte uma string em slug (lowercase, sem caracteres especiais, com hífens).
     *
     * @param s string de entrada
     * @return slug resultante
     */
    protected static String toSlug(String s) {
        return s.toLowerCase()
                .replaceAll("[^a-z0-9áéíóúãõâêîôûçñ\\s]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }

    /**
     * Remove o prefixo "The " do nome do artista, se presente.
     *
     * @param artist nome do artista
     * @return nome sem o prefixo "The ", ou o nome original
     */
    protected static String withoutThe(String artist) {
        if (artist == null) return "";
        String trimmed = artist.trim();
        if (trimmed.toLowerCase().startsWith("the ")) {
            return trimmed.substring(4).trim();
        }
        return trimmed;
    }

    /**
     * Inverte a ordem das palavras do nome do artista e converte em slug.
     *
     * @param artist nome do artista
     * @return slug com palavras invertidas, ou string vazia se inválido
     */
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

    /**
     * Adiciona um slug à lista se não for nulo, vazio ou duplicado.
     *
     * @param slugs lista de slugs acumulados
     * @param slug slug a ser adicionado
     */
    protected static void addSlug(List<String> slugs, String slug) {
        if (slug != null && !slug.isEmpty() && !slugs.contains(slug)) {
            slugs.add(slug);
        }
    }

    /**
     * Gera lista de slugs possíveis para o artista (direto, sem the, invertido).
     *
     * @param artist nome do artista
     * @return lista de slugs gerados
     */
    protected List<String> artistSlugs(String artist) {
        List<String> slugs = new ArrayList<>();
        addSlug(slugs, toSlug(artist));
        addSlug(slugs, toSlug(withoutThe(artist)));
        addSlug(slugs, invertedArtistSlug(artist));
        addSlug(slugs, invertedArtistSlug(withoutThe(artist)));
        return slugs;
    }
}
