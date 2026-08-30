package com.ovelha.fy.dictionary.infrastructure;

import com.ovelha.fy.dictionary.domain.model.DictionaryLookupResult;
import com.ovelha.fy.dictionary.domain.port.DictionarySource;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Classe base abstrata para fontes de dicionário (Template Method).
 * Define o fluxo: montar URL → buscar HTML → extrair definições.
 * Subclasses implementam os hooks para cada dicionário/língua.
 */
public abstract class AbstractDictionarySource implements DictionarySource {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final int TIMEOUT_MS = 10_000;

    /**
     * Fluxo template de busca no dicionário.
     *
     * @param word palavra a ser consultada
     * @return resultado da consulta ou {@code null} se não encontrou
     */
    @Override
    public DictionaryLookupResult lookup(String word) {
        if (word == null || word.isBlank()) return null;
        String url = buildUrl(word);
        log.info("[Dicionário] Buscando \"{}\" em {}", word, url);
        try {
            Document doc = fetchPage(url);
            String pageWord = extractWord(doc, word);
            List<String> definitions = extractDefinitions(doc);
            if (definitions.isEmpty()) {
                log.warn("[Dicionário] Nenhuma definição encontrada para \"{}\" em {}", word, url);
                return null;
            }
            String meanings = String.join("\n", definitions);
            return new DictionaryLookupResult(pageWord, sourceName(), language(), meanings);
        } catch (IOException e) {
            log.error("[Dicionário] Erro ao buscar \"{}\" em {}: {}", word, url, e.getMessage());
            return null;
        }
    }

    /**
     * Monta a URL completa para buscar a palavra no dicionário.
     *
     * @param word palavra codificada para URL
     * @return URL completa da página da palavra
     */
    protected abstract String buildUrl(String word);

    /**
     * Extrai a palavra consultada da página HTML.
     *
     * @param doc  documento Jsoup parseado
     * @param word palavra original consultada (fallback)
     * @return palavra encontrada na página
     */
    protected abstract String extractWord(Document doc, String word);

    /**
     * Extrai as definições/significados da página HTML.
     *
     * @param doc documento Jsoup parseado
     * @return lista de definições encontradas
     */
    protected abstract List<String> extractDefinitions(Document doc);

    /**
     * Realiza a requisição HTTP GET e retorna o documento parseado.
     *
     * @param url URL da página
     * @return documento Jsoup
     * @throws IOException se ocorrer erro de rede ou timeout
     */
    private Document fetchPage(String url) throws IOException {
        Connection.Response res = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; MP3Player/1.0)")
                .timeout(TIMEOUT_MS)
                .ignoreContentType(true)
                .execute();
        return res.parse();
    }
}
