package com.mp3player.playlist.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaylistTest {

    @Test
    void addSongAppendsNewSongAndIgnoresDuplicate() {
        Playlist playlist = new Playlist("Rock", List.of("a.mp3"));
        Playlist withB = playlist.addSong("b.mp3");
        assertEquals(List.of("a.mp3", "b.mp3"), withB.getSongPaths());
        assertEquals(withB, withB.addSong("b.mp3"));
    }

    @Test
    void removeSongDropsItFromList() {
        Playlist playlist = new Playlist("Rock", List.of("a.mp3", "b.mp3"));
        assertEquals(List.of("b.mp3"), playlist.removeSong("a.mp3").getSongPaths());
    }

    @Test
    void sameNameIsSamePlaylist() {
        assertEquals(new Playlist("Rock", List.of("a")), new Playlist("Rock", List.of("b")));
    }
}