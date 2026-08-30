package com.ovelha.fy.dictionary.domain.model;

/**
 * Resultado de uma busca no dicionário.
 *
 * @param word      palavra consultada
 * @param source    nome da fonte/dicionário (ex: "Priberam")
 * @param language  código da língua (ex: "pt")
 * @param meanings  significados encontrados (um por linha)
 */
public record DictionaryLookupResult(String word, String source, String language, String meanings) {
}
