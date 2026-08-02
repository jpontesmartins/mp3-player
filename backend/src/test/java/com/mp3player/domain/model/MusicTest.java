package com.mp3player.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MusicTest {

    @Test
    void toTagMapOmitsBlankFields() {
        Music music = new Music("p.mp3", new Music.Metadata("Titulo", "Artista", null, null, null, null, 123000L));
        Map<String, String> tags = music.toTagMap();
        assertEquals("Titulo", tags.get("title"));
        assertEquals("123000", tags.get("duration_ms"));
        assertFalse(tags.containsKey("album"));
    }

    @Test
    void equalsAndHashCodeBasedOnPath() {
        Music a = new Music("C:\\x.mp3", Music.Metadata.empty());
        Music b = new Music("C:\\x.mp3", new Music.Metadata("T", null, null, null, null, null, null));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}