package com.mp3player.dictionary.domain.port;

import com.mp3player.dictionary.domain.model.DictionaryLookupResult;

/**
 * Port (Strategy) para busca de palavras em dicionários online.
 * Cada implementação Consulta um dicionário específico para uma língua.
 */
public interface DictionarySource {

    /**
     * Retorna o código da língua suportada por esta fonte.
     *
     * @return código ISO da língua (ex: "pt", "en")
     */
    String language();

    /**
     * Retorna o nome legível da fonte do dicionário.
     *
     * @return nome exibido ao usuário (ex: "Priberam")
     */
    String sourceName();

    /**
     * Busca o significado de uma palavra no dicionário.
     *
     * @param word palavra a ser consultada
     * @return resultado da consulta ou {@code null} se não encontrou
     */
    DictionaryLookupResult lookup(String word);
}
