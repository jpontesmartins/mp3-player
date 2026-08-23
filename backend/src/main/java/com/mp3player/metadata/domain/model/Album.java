package com.mp3player.metadata.domain.model;

/**
 * Value object que identifica um álbum pelo nome e pelo artista.
 */
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

    /**
     * Verifica se dois álbuns são iguais (mesmo nome, ignorando caso, e mesmo artista).
     *
     * @param o objeto a ser comparado
     * @return {@code true} se os álbuns são iguais
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Album other)) return false;
        return name.equalsIgnoreCase(other.name)
                && (artist == null ? other.artist == null : artist.equals(other.artist));
    }

    /**
     * Retorna o hash code baseado no nome (minúsculo) e no artista.
     *
     * @return hash code do álbum
     */
    @Override
    public int hashCode() {
        return 31 * name.toLowerCase().hashCode() + (artist == null ? 0 : artist.hashCode());
    }
}