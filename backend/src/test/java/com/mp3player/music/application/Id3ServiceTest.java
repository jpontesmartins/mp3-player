package com.mp3player.music.application;

import com.mp3player.player.domain.model.MusicFile;
import com.mp3player.music.domain.port.Id3Codec;
import com.mp3player.music.domain.repository.MetadataCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Id3ServiceTest {

    @Mock
    Id3Codec id3Codec;

    @Mock
    MetadataCacheRepository cache;

    private Id3Service service;

    @BeforeEach
    void setUp() {
        service = new Id3Service(id3Codec, cache);
    }

    @Test
    void getForFileReturnsTagMap() {
        when(id3Codec.read("a.mp3"))
                .thenReturn(new MusicFile("a.mp3", new MusicFile.Metadata("Titulo", "Artista", "Album", "2020", "Rock", "3", 180000L)));

        Map<String, String> tags = service.getForFile("a.mp3");
        assertEquals("Titulo", tags.get("title"));
        assertEquals("Artista", tags.get("artist"));
        assertEquals("180000", tags.get("duration_ms"));
    }

    @Test
    void getForFilePropagatesException() {
        when(id3Codec.read("bad.mp3")).thenThrow(new RuntimeException("broken"));
        assertThrows(RuntimeException.class, () -> service.getForFile("bad.mp3"));
    }

    @Test
    void bulkReturnsEntryPerPath() {
        when(cache.get("a.mp3")).thenReturn(null);
        when(cache.get("b.mp3")).thenReturn(null);
        when(id3Codec.read("a.mp3")).thenReturn(new MusicFile("a.mp3", new MusicFile.Metadata("A", null, null, null, null, null, null)));
        when(id3Codec.read("b.mp3")).thenReturn(new MusicFile("b.mp3", new MusicFile.Metadata("B", null, null, null, null, null, null)));

        Map<String, Map<String, String>> bulk = service.getBulk(List.of("a.mp3", "b.mp3"), false);
        assertEquals(2, bulk.size());
        assertEquals("A", bulk.get("a.mp3").get("title"));
        assertEquals("B", bulk.get("b.mp3").get("title"));
    }

    @Test
    void bulkServesCachedPathsWithoutReading() {
        when(cache.get("a.mp3")).thenReturn(Map.of("title", "Cached"));
        when(cache.get("b.mp3")).thenReturn(null);
        when(id3Codec.read("b.mp3")).thenReturn(new MusicFile("b.mp3", new MusicFile.Metadata("B", null, null, null, null, null, null)));

        Map<String, Map<String, String>> bulk = service.getBulk(List.of("a.mp3", "b.mp3"), false);
        assertEquals("Cached", bulk.get("a.mp3").get("title"));
        assertEquals("B", bulk.get("b.mp3").get("title"));
        verify(id3Codec, never()).read("a.mp3");
    }

    @Test
    void bulkRefreshesAllWhenRequested() {
        when(id3Codec.read("a.mp3")).thenReturn(new MusicFile("a.mp3", new MusicFile.Metadata("Fresh", null, null, null, null, null, null)));
        when(id3Codec.read("b.mp3")).thenReturn(new MusicFile("b.mp3", new MusicFile.Metadata("B", null, null, null, null, null, null)));

        Map<String, Map<String, String>> bulk = service.getBulk(List.of("a.mp3", "b.mp3"), true);
        assertEquals("Fresh", bulk.get("a.mp3").get("title"));
        assertEquals("B", bulk.get("b.mp3").get("title"));
        verify(id3Codec).read("a.mp3");
        verify(id3Codec).read("b.mp3");
    }

    @Test
    void updateReturnsUpdatedTags() {
        MusicFile updated = new MusicFile("a.mp3", new MusicFile.Metadata("Novo", "Artista", null, null, null, null, 180000L));
        when(id3Codec.update("a.mp3", Map.of("title", "Novo"))).thenReturn(updated);

        Map<String, String> tags = service.update("a.mp3", Map.of("title", "Novo"));
        assertEquals("Novo", tags.get("title"));
    }

    @Test
    void updateFailurePropagatesException() {
        when(id3Codec.update("bad.mp3", Map.of("title", "X")))
                .thenThrow(new IllegalStateException("arquivo corrompido"));

        assertThrows(IllegalStateException.class,
                () -> service.update("bad.mp3", Map.of("title", "X")));
    }

    @Test
    void cacheLocationReturnsLocation() {
        when(cache.location()).thenReturn("/tmp/id3cache");
        assertEquals("/tmp/id3cache", service.cacheLocation());
    }

    @Test
    void getForFileWithNullFieldsReturnsTagMap() {
        when(id3Codec.read("nulls.mp3"))
                .thenReturn(new MusicFile("nulls.mp3", new MusicFile.Metadata(null, null, null, null, null, null, null)));

        Map<String, String> tags = service.getForFile("nulls.mp3");

        assertNull(tags.get("title"));
        assertNull(tags.get("artist"));
        assertNull(tags.get("album"));
    }

    @Test
    void bulkWithEmptyListReturnsEmptyMap() {
        Map<String, Map<String, String>> bulk = service.getBulk(List.of(), false);
        assertTrue(bulk.isEmpty());
    }

    @Test
    void bulkWithRefreshTrueReadsAllFiles() {
        when(id3Codec.read("a.mp3")).thenReturn(new MusicFile("a.mp3", new MusicFile.Metadata("A", null, null, null, null, null, null)));
        when(id3Codec.read("b.mp3")).thenReturn(new MusicFile("b.mp3", new MusicFile.Metadata("B", null, null, null, null, null, null)));

        Map<String, Map<String, String>> bulk = service.getBulk(List.of("a.mp3", "b.mp3"), true);

        assertEquals(2, bulk.size());
        verify(id3Codec).read("a.mp3");
        verify(id3Codec).read("b.mp3");
    }

    @Test
    void updateWithEmptyTagMapReturnsUpdatedTags() {
        MusicFile updated = new MusicFile("a.mp3", new MusicFile.Metadata("Title", "Artist", null, null, null, null, null));
        when(id3Codec.update("a.mp3", Map.of())).thenReturn(updated);

        Map<String, String> tags = service.update("a.mp3", Map.of());

        assertEquals("Title", tags.get("title"));
    }
}
