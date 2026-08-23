package com.mp3player.metadata.domain.port;

import com.mp3player.player.domain.model.Music;

import java.util.Map;

/**
 * Port para leitura e escrita dos metadados ID3 de um arquivo de música.
 * As implementações podem usar mp3agic ou outro codec.
 */
public interface Id3Codec {

    /**
     * Lê os metadados do arquivo no caminho informado e retorna um agregado {@link Music}.
     *
     * @param filePath caminho absoluto do arquivo MP3
     * @return agregado de música com os metadados lidos
     */
    Music read(String filePath);

    /**
     * Atualiza os campos editáveis do arquivo e retorna o {@link Music} atualizado.
     *
     * @param filePath caminho absoluto do arquivo MP3
     * @param tags mapa de tags a serem atualizadas (chave → valor)
     * @return agregado de música com os metadados atualizados
     */
    Music update(String filePath, Map<String, String> tags);
}