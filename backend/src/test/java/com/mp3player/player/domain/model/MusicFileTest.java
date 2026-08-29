package com.mp3player.player.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MusicFileTest {

    @Test
    void toTagMapOmitsBlankFields() {
        MusicFile musicFile = new MusicFile("p.mp3", new MusicFile.Metadata("Titulo", "Artista", null, null, null, null, 123000L));
        Map<String, String> tags = musicFile.toTagMap();
        assertEquals("Titulo", tags.get("title"));
        assertEquals("123000", tags.get("duration_ms"));
        assertFalse(tags.containsKey("album"));
    }

    @Test
    void fromTagsAndToTagMapRoundTripDisc() {
        Map<String, String> tags = Map.of("title", "Titulo", "disc", "1/2", "duration_ms", "180000");
        MusicFile.Metadata metadata = MusicFile.Metadata.fromTags(tags);
        assertEquals("1/2", metadata.disc());
        Map<String, String> out = new MusicFile("p.mp3", metadata).toTagMap();
        assertEquals("1/2", out.get("disc"));
    }

    @Test
    void fromTagsParsesBitrateIntoTagMap() {
        Map<String, String> tags = Map.of("kbps", "320", "duration_ms", "180000");
        MusicFile.Metadata metadata = MusicFile.Metadata.fromTags(tags);
        assertEquals(Integer.valueOf(320), metadata.bitrateKbps());
        Map<String, String> out = new MusicFile("p.mp3", metadata).toTagMap();
        assertEquals("320", out.get("kbps"));
    }

    @Test
    void fromTagsIgnoresInvalidNumbers() {
        Map<String, String> tags = Map.of("kbps", "abc", "duration_ms", "xyz");
        MusicFile.Metadata metadata = MusicFile.Metadata.fromTags(tags);
        assertNull(metadata.bitrateKbps());
        assertNull(metadata.durationMs());
    }

    @Test
    void fromTagsTurnsBlankTextFieldsToNull() {
        MusicFile.Metadata metadata = MusicFile.Metadata.fromTags(Map.of("title", "  ", "artist", "Artista"));
        assertNull(metadata.title());
        assertEquals("Artista", metadata.artist());
    }

@Test
    void toTagMapOmitsBlankAndTrimsValues() {
        MusicFile.Metadata metadata = new MusicFile.Metadata(" Titulo ", null, null, null, null, "3", null, 120000L, 256);
        Map<String, String> tags = new MusicFile("p.mp3", metadata).toTagMap();
        assertEquals("Titulo", tags.get("title"));
        assertFalse(tags.containsKey("artist"));
        assertEquals("3", tags.get("track"));
        assertEquals("120000", tags.get("duration_ms"));
        assertEquals("256", tags.get("kbps"));
    }
}