package com.mp3player.domain.port;

import com.mp3player.domain.model.Music;

import java.util.Map;

/**
 * Port para leitura e escrita dos metadados ID3 de um arquivo de música.
 * As implementações podem usar mp3agic ou outro codec.
 */
public interface Id3Codec {

    /** Lê os metadados do arquivo no caminho informado, retornando um agregado {@link Music}. */
    Music read(String filePath);

    /** Atualiza os campos editáveis e retorna o {@link Music} atualizado. */
    Music update(String filePath, Map<String, String> tags);
}