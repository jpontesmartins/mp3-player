package com.ovelha.fy.lyrics.domain.model;

import java.util.Objects;

/**
 * Entidade de domínio representando a letra de uma única música.
 * Identificada pelo caminho absoluto da música à qual pertence.
 */
public final class Lyric {

    /** Prefixo da mensagem amigável exibida quando nenhuma fonte encontra a letra. */
    public static final String NOT_FOUND_PREFIX = "Letra não encontrada para ";

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

    /**
     * Monta a mensagem amigável de "letra não encontrada".
     *
     * @param artist nome do artista
     * @param title título da música
     * @return mensagem no formato {@code Letra não encontrada para "<título>" de <artista>}
     */
    public static String notFoundMessage(String artist, String title) {
        return NOT_FOUND_PREFIX + "\"" + title + "\" de " + artist;
    }

    /**
     * Indica se o texto é apenas a mensagem de "letra não encontrada" (gravada por
     * versões anteriores como se fosse letra).
     *
     * @param text texto a verificar
     * @return {@code true} se for uma única linha iniciada pelo prefixo da mensagem
     */
    public static boolean isNotFoundMessage(String text) {
        if (text == null) return false;
        String t = text.strip();
        return t.startsWith(NOT_FOUND_PREFIX) && t.indexOf(10) < 0;
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