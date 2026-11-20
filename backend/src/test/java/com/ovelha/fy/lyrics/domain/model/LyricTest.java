package com.ovelha.fy.lyrics.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LyricTest {

    @Test
    void requiresNonNullMusicPath() {
        assertThrows(NullPointerException.class, () -> new Lyric(null, "texto"));
    }

    @Test
    void nullTextBecomesEmpty() {
        Lyric lyric = new Lyric("a.mp3", null);
        assertEquals("", lyric.getText());
    }

    @Test
    void gettersReturnStoredValues() {
        Lyric lyric = new Lyric("C:\\a.mp3", "linha 1\nlinha 2");
        assertEquals("C:\\a.mp3", lyric.getMusicPath());
        assertEquals("linha 1\nlinha 2", lyric.getText());
    }

    @Test
    void identityIsTheMusicPathOnly() {
        Lyric a = new Lyric("a.mp3", "letra original");
        Lyric b = new Lyric("a.mp3", "outra letra");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void notFoundMessageHasExpectedFormat() {
        assertEquals("Letra não encontrada para \"Song\" de Artist", Lyric.notFoundMessage("Artist", "Song"));
    }

    @Test
    void isNotFoundMessageDetectsOnlySingleLineMessage() {
        org.junit.jupiter.api.Assertions.assertTrue(Lyric.isNotFoundMessage(Lyric.notFoundMessage("A", "B")));
        org.junit.jupiter.api.Assertions.assertFalse(Lyric.isNotFoundMessage("Letra não encontrada para x\noutra linha"));
        org.junit.jupiter.api.Assertions.assertFalse(Lyric.isNotFoundMessage(""));
        org.junit.jupiter.api.Assertions.assertFalse(Lyric.isNotFoundMessage(null));
    }

    @Test
    void differentMusicPathsAreNotEqual() {
        assertNotEquals(new Lyric("a.mp3", "x"), new Lyric("b.mp3", "x"));
    }
}