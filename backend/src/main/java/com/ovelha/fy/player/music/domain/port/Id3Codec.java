package com.ovelha.fy.player.music.domain.port;

import com.ovelha.fy.player.domain.model.MusicFile;

import java.util.Map;

/**
 * Port para leitura e escrita dos metadados ID3 de um arquivo de música.
 * As implementações podem usar mp3agic ou outro codec.
 */
public interface Id3Codec {

    /**
     * Lê os metadados do arquivo no caminho informado e retorna um agregado {@link MusicFile}.
     *
     * @param filePath caminho absoluto do arquivo MP3
     * @return agregado de música com os metadados lidos
     */
    MusicFile read(String filePath);

    /**
     * Atualiza os campos editáveis do arquivo e retorna o {@link MusicFile} atualizado.
     *
     * @param filePath caminho absoluto do arquivo MP3
     * @param tags mapa de tags a serem atualizadas (chave → valor)
     * @return agregado de música com os metadados atualizados
     */
    MusicFile update(String filePath, Map<String, String> tags);
}