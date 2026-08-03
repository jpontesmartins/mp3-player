package com.mp3player.domain.model;

import java.util.Objects;

/**
 * Entidade de domínio representando a letra de uma única música.
 * Identificada pelo caminho absoluto da música à qual pertence.
 */
public final class Lyric {

    private final String musicPath;
    private final String text;

    public Lyric(String musicPath, String text) {
        this.musicPath = Objects.requireNonNull(musicPath);
        this.text = text == null ? "" : text;
    }

    public String getMusicPath() {
        return musicPath;
    }

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