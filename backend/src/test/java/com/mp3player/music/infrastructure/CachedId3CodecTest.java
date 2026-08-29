package com.mp3player.music.infrastructure;

import com.mp3player.player.domain.model.MusicFile;
import com.mp3player.music.domain.port.Id3Codec;
import com.mp3player.music.domain.repository.MetadataCacheRepository;
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

        MusicFile result = cached.read("a.mp3");

        assertEquals("Cached", result.getMetadata().title());
        assertEquals("Artist", result.getMetadata().artist());
        verify(delegate, never()).read("a.mp3");
    }

    @Test
    void readReReadsWhenTimestampDiffers() {
        Map<String, String> staleTags = Map.of("title", "Old", "_lastModified", "1000");
        when(cache.get("a.mp3")).thenReturn(staleTags);

        MusicFile fresh = new MusicFile("a.mp3", new MusicFile.Metadata("Fresh", "Artist", null, null, null, null, null));
        when(delegate.read("a.mp3")).thenReturn(fresh);

        MusicFile result = cached.read("a.mp3");

        assertEquals("Fresh", result.getMetadata().title());
        verify(delegate).read("a.mp3");
    }

    @Test
    void readReReadsWhenNoTimestamp() {
        Map<String, String> oldCache = Map.of("title", "Old");
        when(cache.get("a.mp3")).thenReturn(oldCache);

        MusicFile fresh = new MusicFile("a.mp3", new MusicFile.Metadata("Fresh", null, null, null, null, null, null));
        when(delegate.read("a.mp3")).thenReturn(fresh);

        MusicFile result = cached.read("a.mp3");

        assertEquals("Fresh", result.getMetadata().title());
        verify(delegate).read("a.mp3");
    }

    @Test
    void readDelegatesAndCachesWhenMiss() {
        when(cache.get("a.mp3")).thenReturn(null);
        MusicFile musicFile = new MusicFile("a.mp3", new MusicFile.Metadata("Fresh", "Artist", null, null, null, null, null));
        when(delegate.read("a.mp3")).thenReturn(musicFile);

        MusicFile result = cached.read("a.mp3");

        assertEquals("Fresh", result.getMetadata().title());
        verify(delegate).read("a.mp3");
        verify(cache).put(eq("a.mp3"), anyMap());
    }

    @Test
    void updateDelegatesAndCachesResult() {
        MusicFile updated = new MusicFile("a.mp3", new MusicFile.Metadata("Updated", "Artist", null, null, null, null, null));
        when(delegate.update("a.mp3", Map.of("title", "Updated"))).thenReturn(updated);

        MusicFile result = cached.update("a.mp3", Map.of("title", "Updated"));

        assertEquals("Updated", result.getMetadata().title());
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
