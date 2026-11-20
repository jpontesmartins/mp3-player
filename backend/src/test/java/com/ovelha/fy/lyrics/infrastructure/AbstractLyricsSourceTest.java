package com.ovelha.fy.lyrics.infrastructure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testes dos utilitários de slug de {@link AbstractLyricsSource}.
 */
class AbstractLyricsSourceTest {

    @Test
    void removesDiacritics() {
        assertEquals("nara-leao", AbstractLyricsSource.toSlug("Nara Leão"));
        assertEquals("miserere-nobis", AbstractLyricsSource.toSlug("Miserere Nóbis"));
    }

    @Test
    void keepsPlainNamesUnchanged() {
        assertEquals("iggy-pop", AbstractLyricsSource.toSlug("Iggy Pop"));
        assertEquals("the-passenger", AbstractLyricsSource.toSlug("The Passenger"));
    }

    @Test
    void trimsEdgeHyphensAndCollapsesRepeats() {
        assertEquals("song", AbstractLyricsSource.toSlug("Song -"));
        assertEquals("intro", AbstractLyricsSource.toSlug("- Intro"));
        assertEquals("a-b", AbstractLyricsSource.toSlug("A  -  B"));
    }
}
