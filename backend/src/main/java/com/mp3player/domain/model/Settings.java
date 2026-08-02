package com.mp3player.domain.model;

import java.util.Objects;

/** User preferences for the player. */
public final class Settings {

    public enum PlaybackMode { CONTINUOUS, SHUFFLE, REPEAT }

    private final PlaybackMode playbackMode;
    private final boolean showCover;

    public Settings(PlaybackMode playbackMode, boolean showCover) {
        this.playbackMode = Objects.requireNonNullElse(playbackMode, PlaybackMode.CONTINUOUS);
        this.showCover = showCover;
    }

    public static Settings defaults() {
        return new Settings(PlaybackMode.CONTINUOUS, true);
    }

    public PlaybackMode getPlaybackMode() {
        return playbackMode;
    }

    public boolean isShowCover() {
        return showCover;
    }

    public Settings withPlaybackMode(PlaybackMode mode) {
        return new Settings(mode, showCover);
    }

    public Settings withShowCover(boolean cover) {
        return new Settings(playbackMode, cover);
    }
}