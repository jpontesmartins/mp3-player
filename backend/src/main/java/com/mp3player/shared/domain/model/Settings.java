package com.mp3player.shared.domain.model;

import java.util.Objects;

/** Preferências do usuário para o player. */
public final class Settings {

    /**
     * Modos de reprodução disponíveis no player.
     */
    public enum PlaybackMode { CONTINUOUS, SHUFFLE, REPEAT }

    private final PlaybackMode playbackMode;
    private final boolean showCover;

    /**
     * Cria uma instância de configurações com os valores informados.
     *
     * @param playbackMode modo de reprodução; {@code null} assume {@link PlaybackMode#CONTINUOUS}
     * @param showCover    {@code true} para exibir a capa do álbum
     */
    public Settings(PlaybackMode playbackMode, boolean showCover) {
        this.playbackMode = Objects.requireNonNullElse(playbackMode, PlaybackMode.CONTINUOUS);
        this.showCover = showCover;
    }

    /** Retorna as configurações padrão (reprodução contínua e capa visível). */
    public static Settings defaults() {
        return new Settings(PlaybackMode.CONTINUOUS, true);
    }

    /**
     * Retorna o modo de reprodução configurado.
     *
     * @return modo de reprodução atual
     */
    public PlaybackMode getPlaybackMode() {
        return playbackMode;
    }

    /**
     * Indica se a capa do álbum deve ser exibida.
     *
     * @return {@code true} se a capa está habilitada
     */
    public boolean isShowCover() {
        return showCover;
    }

    /** Retorna uma cópia com o modo de reprodução alterado. */
    public Settings withPlaybackMode(PlaybackMode mode) {
        return new Settings(mode, showCover);
    }

    /** Retorna uma cópia com a preferência de exibir a capa alterada. */
    public Settings withShowCover(boolean cover) {
        return new Settings(playbackMode, cover);
    }
}