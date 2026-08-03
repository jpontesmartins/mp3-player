package com.mp3player.domain.model;

import java.util.Objects;

/** Preferências do usuário para o player. */
public final class Settings {

    public enum PlaybackMode { CONTINUOUS, SHUFFLE, REPEAT }

    private final PlaybackMode playbackMode;
    private final boolean showCover;

    public Settings(PlaybackMode playbackMode, boolean showCover) {
        this.playbackMode = Objects.requireNonNullElse(playbackMode, PlaybackMode.CONTINUOUS);
        this.showCover = showCover;
    }

    /** Retorna as configurações padrão (reprodução contínua e capa visível). */
    public static Settings defaults() {
        return new Settings(PlaybackMode.CONTINUOUS, true);
    }

    public PlaybackMode getPlaybackMode() {
        return playbackMode;
    }

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