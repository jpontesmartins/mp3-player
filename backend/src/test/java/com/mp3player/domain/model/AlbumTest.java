package com.mp3player.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlbumTest {

    @Test
    void trimsName() {
        Album album = new Album(new Artist("Titãs"), "  Acústico  ");
        assertEquals("Acústico", album.getName());
    }

    @Test
    void equalityIsCaseInsensitiveForNameAndArtist() {
        Album a = new Album(new Artist("titãs"), "acústico");
        Album b = new Album(new Artist("Titãs"), "Acústico");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentArtistMakesAlbumsDifferent() {
        Album a = new Album(new Artist("Titãs"), "Acústico");
        Album b = new Album(new Artist("Legião"), "Acústico");
        assertNotEquals(a, b);
    }

    @Test
    void differentNameMakesAlbumsDifferent() {
        Album a = new Album(new Artist("Titãs"), "Acústico");
        Album b = new Album(new Artist("Titãs"), "Sempre");
        assertNotEquals(a, b);
    }

    @Test
    void albumsWithNullArtistAreEqual() {
        assertEquals(new Album(null, "X"), new Album(null, "X"));
    }

    @Test
    void exposesArtistAndName() {
        Artist artist = new Artist("Titãs");
        Album album = new Album(artist, "Acústico");
        assertSame(artist, album.getArtist());
        assertEquals("Acústico", album.getName());
    }
}