package com.mp3player.metadata.domain.model;

/**
 * Representa uma imagem de capa baixada da web, com os bytes e o tipo de
 * conteúdo informado pelo servidor de origem.
 */
public record CoverImage(byte[] bytes, String contentType) {

    public boolean isEmpty() {
        return bytes == null || bytes.length == 0;
    }
}