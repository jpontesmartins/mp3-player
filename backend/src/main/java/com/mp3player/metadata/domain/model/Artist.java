package com.mp3player.metadata.domain.model;

/**
 * Entidade de domínio representando um artista (único por nome).
 */
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

    /**
     * Verifica se dois artistas são iguais (mesmo nome, ignorando caso).
     *
     * @param o objeto a ser comparado
     * @return {@code true} se os artistas são iguais
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Artist other)) return false;
        return name.equalsIgnoreCase(other.name);
    }

    /**
     * Retorna o hash code baseado no nome (minúsculo).
     *
     * @return hash code do artista
     */
    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }
}