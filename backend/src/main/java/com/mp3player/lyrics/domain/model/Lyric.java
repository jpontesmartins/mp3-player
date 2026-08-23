package com.mp3player.lyrics.domain.model;

import java.util.Objects;

/**
 * Entidade de domínio representando a letra de uma única música.
 * Identificada pelo caminho absoluto da música à qual pertence.
 */
public final class Lyric {

    private final String musicPath;
    private final String text;

    /**
     * Cria uma nova instância de {@link Lyric}.
     *
     * @param musicPath caminho absoluto do arquivo de áudio
     * @param text texto da letra (pode ser {@code null}, será convertido para "")
     */
    public Lyric(String musicPath, String text) {
        this.musicPath = Objects.requireNonNull(musicPath);
        this.text = text == null ? "" : text;
    }

    /**
     * Retorna o caminho do arquivo de áudio associado a esta letra.
     *
     * @return caminho absoluto do arquivo de áudio
     */
    public String getMusicPath() {
        return musicPath;
    }

    /**
     * Retorna o texto da letra.
     *
     * @return texto da letra
     */
    public String getText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lyric other)) return false;
        return musicPath.equals(other.musicPath);
    }

    @Override
    public int hashCode() {
        return musicPath.hashCode();
    }
}