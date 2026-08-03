package com.mp3player.domain.port;

import com.mp3player.domain.model.CoverImage;

import java.io.IOException;

/**
 * Port para busca e download de uma imagem de capa de álbum na web.
 * A implementação pode usar Google Images, DuckDuckGo ou outro buscador.
 */
public interface AlbumCoverSearcher {

    /**
     * Busca a primeira imagem para o texto informado e retorna os bytes baixados,
     * ou {@code null} se nenhuma imagem for encontrada.
     */
    CoverImage findCover(String query) throws IOException;
}