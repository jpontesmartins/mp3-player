package com.mp3player.music.domain.repository;

import java.util.Map;

/**
 * Porta de persistência do cache de metadados ID3. Mantém as tags lidas por
 * arquivo entre execuções, evitando reler todos os MP3s a cada abertura do
 * programa. O cache é considerado confiável até uma recarga explícita.
 */
public interface MetadataCacheRepository {

    /**
     * Retorna as tags em cache para o caminho informado.
     *
     * @param path caminho do arquivo
     * @return mapa de tags, ou {@code null} se não estiver em cache
     */
    Map<String, String> get(String path);

    /**
     * Armazena as tags do arquivo no cache.
     *
     * @param path caminho do arquivo
     * @param tags mapa de tags a serem armazenadas
     */
    void put(String path, Map<String, String> tags);

    /**
     * Armazena várias entradas de uma vez.
     *
     * @param tagsByPath mapa de caminho → tags
     */
    void putAll(Map<String, Map<String, String>> tagsByPath);

    /**
     * Retorna a localização do arquivo onde o cache é persistido.
     *
     * @return caminho do arquivo de cache
     */
    String location();
}
