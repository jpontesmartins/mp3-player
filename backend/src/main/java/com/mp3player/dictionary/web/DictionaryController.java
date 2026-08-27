package com.mp3player.dictionary.web;

import com.mp3player.dictionary.application.DictionaryLookupService;
import com.mp3player.dictionary.domain.model.DictionaryLookupResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador HTTP para consultas a dicionários online.
 */
@RestController
public class DictionaryController {

    private final DictionaryLookupService lookupService;

    /**
     * Construtor do controlador de dicionário.
     *
     * @param lookupService serviço de consulta a dicionários
     */
    public DictionaryController(DictionaryLookupService lookupService) {
        this.lookupService = lookupService;
    }

    /**
     * Requisição de consulta ao dicionário.
     *
     * @param word     palavra a ser consultada
     * @param language código da língua (ex: "pt")
     */
    public record DictionaryLookupRequest(String word, String language) {}

    /**
     * Busca o significado de uma palavra no dicionário da língua especificada.
     *
     * @param request requisição com a palavra e a língua
     * @return resultado da consulta ou 404 se não encontrou
     */
    @PostMapping("/dictionary/lookup")
    public ResponseEntity<DictionaryLookupResult> lookup(@RequestBody DictionaryLookupRequest request) {
        if (request.word() == null || request.word().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.language() == null || request.language().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        DictionaryLookupResult result = lookupService.lookup(request.word(), request.language());
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Retorna as línguas suportadas pelo dicionário.
     *
     * @return lista de códigos de línguas
     */
    @GetMapping("/dictionary/languages")
    public ResponseEntity<List<String>> languages() {
        return ResponseEntity.ok(lookupService.supportedLanguages());
    }
}
