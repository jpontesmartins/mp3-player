package com.mp3player.music.domain.model;

import lombok.EqualsAndHashCode;

/**
 * Entidade de domínio representando um artista
 */
@EqualsAndHashCode
public final class Artist {

    private final String name;

    /**
     * Construtor do artista.
     *
     * @param name nome do artista, pode ser {@code null}
     */
    public Artist(String name) {
        this.name = name == null ? "" : name.trim();
    }

    /**
     * Retorna o nome do artista.
     *
     * @return nome do artista
     */
    public String getName() {
        return name;
    }

}