package com.mp3player.music.domain.model;

import lombok.EqualsAndHashCode;

/**
 * Entidade de domínio que identifica um álbum pelo nome e pelo artista.
 */
@EqualsAndHashCode
public final class Album {

    private final Artist artist;
    private final String name;

    /**
     * Construtor do álbum.
     *
     * @param artist artista do álbum, pode ser {@code null}
     * @param name nome do álbum, pode ser {@code null}
     */
    public Album(Artist artist, String name) {
        this.artist = artist;
        this.name = name == null ? "" : name.trim();
    }

    /**
     * Retorna o artista do álbum.
     *
     * @return artista do álbum, pode ser {@code null}
     */
    public Artist getArtist() {
        return artist;
    }

    /**
     * Retorna o nome do álbum.
     *
     * @return nome do álbum
     */
    public String getName() {
        return name;
    }

}