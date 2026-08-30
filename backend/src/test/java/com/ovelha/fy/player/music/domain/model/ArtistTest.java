package com.ovelha.fy.player.music.domain.model;

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
    void differentNamesAreNotEqual() {
        assertNotEquals(new Artist("Titãs"), new Artist("Legião Urbana"));
    }
}