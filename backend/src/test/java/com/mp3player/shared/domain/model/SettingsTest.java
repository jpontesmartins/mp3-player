package com.mp3player.shared.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SettingsTest {

    @Test
    void defaultsAreContinuousAndShowCover() {
        Settings settings = Settings.defaults();
        assertEquals(Settings.PlaybackMode.CONTINUOUS, settings.getPlaybackMode());
        assertTrue(settings.isShowCover());
    }

    @Test
    void nullPlaybackModeFallsBackToContinuous() {
        Settings settings = new Settings(null, false);
        assertEquals(Settings.PlaybackMode.CONTINUOUS, settings.getPlaybackMode());
    }

    @Test
    void storesGivenModeAndCoverPreference() {
        Settings settings = new Settings(Settings.PlaybackMode.SHUFFLE, false);
        assertEquals(Settings.PlaybackMode.SHUFFLE, settings.getPlaybackMode());
        assertFalse(settings.isShowCover());
    }

    @Test
    void withPlaybackModeKeepsCoverPreference() {
        Settings settings = new Settings(Settings.PlaybackMode.CONTINUOUS, false);
        Settings changed = settings.withPlaybackMode(Settings.PlaybackMode.REPEAT);
        assertEquals(Settings.PlaybackMode.REPEAT, changed.getPlaybackMode());
        assertFalse(changed.isShowCover());
        assertEquals(Settings.PlaybackMode.CONTINUOUS, settings.getPlaybackMode());
    }

    @Test
    void withShowCoverKeepsPlaybackMode() {
        Settings settings = new Settings(Settings.PlaybackMode.REPEAT, true);
        Settings changed = settings.withShowCover(false);
        assertFalse(changed.isShowCover());
        assertEquals(Settings.PlaybackMode.REPEAT, changed.getPlaybackMode());
        assertTrue(settings.isShowCover());
    }
}