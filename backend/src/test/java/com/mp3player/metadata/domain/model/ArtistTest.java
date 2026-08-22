package com.mp3player.metadata.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArtistTest {

    @Test
    void trimsName() {
        Artist artist = new Artist("  Legião Urbana  ");
        assertEquals("Legião Urbana", artist.getName());
    }

    @Test
    void nullNameBecomesEmpty() {
        assertEquals("", new Artist(null).getName());
    }

    @Test
    void equalityIsCaseInsensitive() {
        Artist a = new Artist("Titãs");
        Artist b = new Artist("titãs");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentNamesAreNotEqual() {
        assertNotEquals(new Artist("Titãs"), new Artist("Legião Urbana"));
    }
}