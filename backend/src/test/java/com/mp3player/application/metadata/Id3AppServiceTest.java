package com.mp3player.application.metadata;

import com.mp3player.domain.model.Music;
import com.mp3player.domain.port.Id3Codec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Id3AppServiceTest {

    @Mock
    Id3Codec id3Codec;

    @Test
    void getForFileReturnsTagMap() {
        when(id3Codec.read("a.mp3"))
                .thenReturn(new Music("a.mp3", new Music.Metadata("Titulo", "Artista", "Album", "2020", "Rock", "3", 180000L)));

        Id3AppService service = new Id3AppService(id3Codec);
        Map<String, String> tags = service.getForFile("a.mp3");
        assertEquals("Titulo", tags.get("title"));
        assertEquals("Artista", tags.get("artist"));
        assertEquals("180000", tags.get("duration_ms"));
    }

    @Test
    void bulkReturnsEntryPerPath() {
        when(id3Codec.read("a.mp3")).thenReturn(new Music("a.mp3", new Music.Metadata("A", null, null, null, null, null, null)));
        when(id3Codec.read("b.mp3")).thenReturn(new Music("b.mp3", new Music.Metadata("B", null, null, null, null, null, null)));

        Id3AppService service = new Id3AppService(id3Codec);
        Map<String, Map<String, String>> bulk = service.getBulk(List.of("a.mp3", "b.mp3"));
        assertEquals(2, bulk.size());
        assertEquals("A", bulk.get("a.mp3").get("title"));
        assertEquals("B", bulk.get("b.mp3").get("title"));
    }

    @Test
    void readFailureYieldsErrorTag() {
        when(id3Codec.read("bad.mp3")).thenThrow(new RuntimeException("broken"));
        Id3AppService service = new Id3AppService(id3Codec);
        assertEquals("Could not read ID3 tags", service.getForFile("bad.mp3").get("error"));
    }
}