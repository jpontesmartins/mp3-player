package com.mp3player.dictionary.infrastructure;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Fonte de dicionário para Português, utilizando o Priberam.
 * Busca em https://dicionario.priberam.org/{palavra}
 */
@Component
public class PriberamSource extends AbstractDictionarySource {

    private static final String BASE_URL = "https://dicionario.priberam.org/";
    private static final String DEFINITION_CONTAINER = ".dp-definicao";
    private static final String DEFINITION_LINE = "py-4 dp-definicao-linha";

    /**
     * {@inheritDoc}
     *
     * @return código da língua: "pt"
     */
    @Override
    public String language() {
        return "pt";
    }

    /**
     * {@inheritDoc}
     *
     * @return nome da fonte: "Priberam"
     */
    @Override
    public String sourceName() {
        return "Priberam";
    }

    /**
     * Constrói a URL de consulta no Priberam para a palavra informada.
     *
     * @param word palavra a ser consultada
     * @return URL completa da página da palavra
     */
    @Override
    protected String buildUrl(String word) {
        return BASE_URL + word;
    }

    /**
     * Retorna a palavra consultada, conforme extraída do documento.
     *
     * @param doc documento HTML da página da palavra
     * @param word palavra original consultada
     * @return palavra extraída
     */
    @Override
    protected String extractWord(Document doc, String word) {
        return word;
    }

    /**
     * Extrai as definições da palavra a partir do documento HTML.
     *
     * @param doc documento HTML da página da palavra
     * @return lista de definições encontradas
     */
    @Override
    protected List<String> extractDefinitions(Document doc) {
        Element container = doc.selectFirst(DEFINITION_CONTAINER);
        if (container == null) return Collections.emptyList();
        Elements lines = container.getElementsByClass(DEFINITION_LINE);
        return lines.stream()
                .map(Element::text)
                .filter(t -> t != null && !t.isBlank())
                .toList();
    }
}
