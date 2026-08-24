package com.mp3player.dictionary.application;

import com.mp3player.dictionary.domain.model.DictionaryLookupResult;
import com.mp3player.dictionary.domain.port.DictionarySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service de aplicação para consultas a dicionários online.
 * Utiliza Strategy (via {@link DictionarySource}) para selecionar a fonte correta.
 */
@Service
public class DictionaryLookupService {

    private static final Logger log = LoggerFactory.getLogger(DictionaryLookupService.class);

    private final Map<String, DictionarySource> sourcesByLanguage;

    /**
     * Construtor do serviço de consulta a dicionários.
     * Injeta todas as implementações de {@link DictionarySource} e indexa por língua.
     *
     * @param sources lista de fontes de dicionário disponíveis
     */
    public DictionaryLookupService(List<DictionarySource> sources) {
        this.sourcesByLanguage = sources.stream()
                .collect(Collectors.toMap(DictionarySource::language, Function.identity()));
        log.info("[Dicionário] Fontes registradas: {}", sourcesByLanguage.keySet());
    }

    /**
     * Busca o significado de uma palavra no dicionário da língua especificada.
     *
     * @param word     palavra a ser consultada
     * @param language código da língua (ex: "pt")
     * @return resultado da consulta ou {@code null} se não encontrou
     */
    public DictionaryLookupResult lookup(String word, String language) {
        DictionarySource source = sourcesByLanguage.get(language);
        if (source == null) {
            log.warn("[Dicionário] Língua não suportada: {}", language);
            return null;
        }
        return source.lookup(word);
    }

    /**
     * Retorna a lista de línguas suportadas.
     *
     * @return códigos de línguas disponíveis
     */
    public List<String> supportedLanguages() {
        return List.copyOf(sourcesByLanguage.keySet());
    }
}
