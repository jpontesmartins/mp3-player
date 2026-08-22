package com.mp3player.metadata.domain.repository;

import java.util.Map;

/**
 * Porta de persistência do cache de metadados ID3. Mantém as tags lidas por
 * arquivo entre execuções, evitando reler todos os MP3s a cada abertura do
 * programa. O cache é considerado confiável até uma recarga explícita.
 */
public interface MetadataCacheRepository {

    /** Retorna as tags em cache para o caminho, ou {@code null} se não estiver em cache. */
    Map<String, String> get(String path);

    /** Armazena as tags do arquivo no cache. */
    void put(String path, Map<String, String> tags);

    /** Armazena várias entradas de uma vez. */
    void putAll(Map<String, Map<String, String>> tagsByPath);

    /** Localização do arquivo onde o cache é persistido. */
    String location();
}
