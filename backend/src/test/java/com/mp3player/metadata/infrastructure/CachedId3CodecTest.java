package com.mp3player.metadata.infrastructure;

import com.mp3player.player.domain.model.Music;
import com.mp3player.metadata.domain.port.Id3Codec;
import com.mp3player.metadata.domain.repository.MetadataCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachedId3CodecTest {

    @Mock
    Id3Codec delegate;

    @Mock
    MetadataCacheRepository cache;

    private CachedId3Codec cached;

    @BeforeEach
    void setUp() {
        cached = new CachedId3Codec(delegate, cache);
    }

    @Test
    void readReturnsCachedMetadataWhenTimestampMatches() {
        Map<String, String> cachedTags = Map.of("title", "Cached", "artist", "Artist", "_lastModified", "0");
        when(cache.get("a.mp3")).thenReturn(cachedTags);

        Music result = cached.read("a.mp3");

        assertEquals("Cached", result.getMetadata().getTitle());
        assertEquals("Artist", result.getMetadata().getArtist());
        verify(delegate, never()).read("a.mp3");
    }

    @Test
    void readReReadsWhenTimestampDiffers() {
        Map<String, String> staleTags = Map.of("title", "Old", "_lastModified", "1000");
        when(cache.get("a.mp3")).thenReturn(staleTags);

        Music fresh = new Music("a.mp3", new Music.Metadata("Fresh", "Artist", null, null, null, null, null));
        when(delegate.read("a.mp3")).thenReturn(fresh);

        Music result = cached.read("a.mp3");

        assertEquals("Fresh", result.getMetadata().getTitle());
        verify(delegate).read("a.mp3");
    }

    @Test
    void readReReadsWhenNoTimestamp() {
        Map<String, String> oldCache = Map.of("title", "Old");
        when(cache.get("a.mp3")).thenReturn(oldCache);

        Music fresh = new Music("a.mp3", new Music.Metadata("Fresh", null, null, null, null, null, null));
        when(delegate.read("a.mp3")).thenReturn(fresh);

        Music result = cached.read("a.mp3");

        assertEquals("Fresh", result.getMetadata().getTitle());
        verify(delegate).read("a.mp3");
    }

    @Test
    void readDelegatesAndCachesWhenMiss() {
        when(cache.get("a.mp3")).thenReturn(null);
        Music music = new Music("a.mp3", new Music.Metadata("Fresh", "Artist", null, null, null, null, null));
        when(delegate.read("a.mp3")).thenReturn(music);

        Music result = cached.read("a.mp3");

        assertEquals("Fresh", result.getMetadata().getTitle());
        verify(delegate).read("a.mp3");
        verify(cache).put(eq("a.mp3"), anyMap());
    }

    @Test
    void updateDelegatesAndCachesResult() {
        Music updated = new Music("a.mp3", new Music.Metadata("Updated", "Artist", null, null, null, null, null));
        when(delegate.update("a.mp3", Map.of("title", "Updated"))).thenReturn(updated);

        Music result = cached.update("a.mp3", Map.of("title", "Updated"));

        assertEquals("Updated", result.getMetadata().getTitle());
        verify(cache).put(eq("a.mp3"), anyMap());
    }

    @Test
    void updatePropagatesException() {
        when(delegate.update("bad.mp3", Map.of("title", "X")))
                .thenThrow(new IllegalStateException("corrupted"));

        assertThrows(IllegalStateException.class,
                () -> cached.update("bad.mp3", Map.of("title", "X")));
        verify(cache, never()).put(anyString(), anyMap());
    }
}
