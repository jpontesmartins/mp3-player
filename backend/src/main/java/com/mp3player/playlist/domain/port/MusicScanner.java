package com.mp3player.playlist.domain.port;

import com.mp3player.player.domain.model.MusicFile;

import java.io.IOException;
import java.util.List;

/**
 * Port para escanear uma pasta física e retornar todas as músicas encontradas
 * nela, recursivamente. É a fonte da "playlist física" e da biblioteca.
 */
public interface MusicScanner {

    /**
     * Escaneia a pasta informada e retorna as músicas encontradas.
     *
     * @param folderPath caminho absoluto da pasta.
     * @return lista de músicas encontradas.
     * @throws IOException se a pasta não existir ou não puder ser lida.
     */
    List<MusicFile> scanFolder(String folderPath) throws IOException;
}