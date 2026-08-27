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

    @Test
    void defaultsHasContinuousModeAndShowCoverTrue() {
        Settings d = Settings.defaults();
        assertEquals(Settings.PlaybackMode.CONTINUOUS, d.getPlaybackMode());
        assertTrue(d.isShowCover());
    }

    @Test
    void withPlaybackModeReturnsCopyWithNewMode() {
        Settings original = Settings.defaults();
        Settings shuffled = original.withPlaybackMode(Settings.PlaybackMode.SHUFFLE);

        assertEquals(Settings.PlaybackMode.SHUFFLE, shuffled.getPlaybackMode());
        assertEquals(Settings.PlaybackMode.CONTINUOUS, original.getPlaybackMode());
    }

    @Test
    void withShowCoverReturnsCopyWithNewValue() {
        Settings original = Settings.defaults();
        Settings noCover = original.withShowCover(false);

        assertFalse(noCover.isShowCover());
        assertTrue(original.isShowCover());
    }

    @Test
    void withPlaybackModeNullDefaultsToContinuous() {
        Settings settings = new Settings(Settings.PlaybackMode.SHUFFLE, false);
        Settings changed = settings.withPlaybackMode(null);

        assertEquals(Settings.PlaybackMode.CONTINUOUS, changed.getPlaybackMode());
    }

    @Test
    void allPlaybackModesAreUsable() {
        for (Settings.PlaybackMode mode : Settings.PlaybackMode.values()) {
            Settings settings = new Settings(mode, true);
            assertEquals(mode, settings.getPlaybackMode());
        }
    }
}