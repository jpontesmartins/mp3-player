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

    @Test
    void fromTagsAndToTagMapRoundTripDisc() {
        Map<String, String> tags = Map.of("title", "Titulo", "disc", "1/2", "duration_ms", "180000");
        Music.Metadata metadata = Music.Metadata.fromTags(tags);
        assertEquals("1/2", metadata.getDisc());
        Map<String, String> out = new Music("p.mp3", metadata).toTagMap();
        assertEquals("1/2", out.get("disc"));
    }

    @Test
    void fromTagsParsesBitrateIntoTagMap() {
        Map<String, String> tags = Map.of("kbps", "320", "duration_ms", "180000");
        Music.Metadata metadata = Music.Metadata.fromTags(tags);
        assertEquals(Integer.valueOf(320), metadata.getBitrateKbps());
        Map<String, String> out = new Music("p.mp3", metadata).toTagMap();
        assertEquals("320", out.get("kbps"));
    }

    @Test
    void fromTagsIgnoresInvalidNumbers() {
        Map<String, String> tags = Map.of("kbps", "abc", "duration_ms", "xyz");
        Music.Metadata metadata = Music.Metadata.fromTags(tags);
        assertNull(metadata.getBitrateKbps());
        assertNull(metadata.getDurationMs());
    }

    @Test
    void fromTagsTurnsBlankTextFieldsToNull() {
        Music.Metadata metadata = Music.Metadata.fromTags(Map.of("title", "  ", "artist", "Artista"));
        assertNull(metadata.getTitle());
        assertEquals("Artista", metadata.getArtist());
    }

@Test
    void toTagMapOmitsBlankAndTrimsValues() {
        Music.Metadata metadata = new Music.Metadata(" Titulo ", null, null, null, null, "3", null, 120000L, 256);
        Map<String, String> tags = new Music("p.mp3", metadata).toTagMap();
        assertEquals("Titulo", tags.get("title"));
        assertFalse(tags.containsKey("artist"));
        assertEquals("3", tags.get("track"));
        assertEquals("120000", tags.get("duration_ms"));
        assertEquals("256", tags.get("kbps"));
    }
}