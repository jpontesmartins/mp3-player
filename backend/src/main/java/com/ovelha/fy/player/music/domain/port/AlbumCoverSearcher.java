package com.ovelha.fy.player.music.domain.port;

import com.ovelha.fy.player.music.domain.model.CoverImage;

import java.io.IOException;

/**
 * Port para busca e download de uma imagem de capa de álbum na web.
 */
public interface AlbumCoverSearcher {

    /**
     * Busca a primeira imagem de capa para o termo de busca informado.
     *
     * @param query termo de busca (ex: "artista álbum")
     * @return imagem de capa encontrada, ou {@code null} se nenhuma for encontrada
     * @throws IOException se ocorrer erro de rede ou leitura
     */
    CoverImage findCover(String query) throws IOException;
}
