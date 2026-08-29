package com.mp3player.music.domain.model;

/**
 * Representa uma imagem de capa, com os bytes e o tipo de
 * conteúdo informado pelo servidor de origem.
 */
public record CoverImage(byte[] bytes, String contentType) {

    /**
     * Verifica se a imagem está vazia (bytes nulos ou array vazio).
     *
     * @return {@code true} se a imagem estiver vazia
     */
    public boolean isEmpty() {
        return bytes == null || bytes.length == 0;
    }
}