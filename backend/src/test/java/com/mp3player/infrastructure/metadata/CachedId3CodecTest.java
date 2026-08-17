package com.mp3player.infrastructure.metadata;

import com.mp3player.domain.model.Music;
import com.mp3player.domain.port.Id3Codec;
import com.mp3player.domain.repository.MetadataCacheRepository;
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
    void readReturnsCachedMetadataWhenAvailable() {
        when(cache.get("a.mp3")).thenReturn(Map.of("title", "Cached", "artist", "Artist"));

        Music result = cached.read("a.mp3");

        assertEquals("Cached", result.getMetadata().getTitle());
        assertEquals("Artist", result.getMetadata().getArtist());
        verify(delegate, never()).read("a.mp3");
    }

    @Test
    void readDelegatesAndCachesWhenMiss() {
        when(cache.get("a.mp3")).thenReturn(null);
        Music music = new Music("a.mp3", new Music.Metadata("Fresh", "Artist", null, null, null, null, null));
        when(delegate.read("a.mp3")).thenReturn(music);

        Music result = cached.read("a.mp3");

        assertEquals("Fresh", result.getMetadata().getTitle());
        verify(delegate).read("a.mp3");
        verify(cache).put("a.mp3", music.toTagMap());
    }

    @Test
    void updateDelegatesAndCachesResult() {
        Music updated = new Music("a.mp3", new Music.Metadata("Updated", "Artist", null, null, null, null, null));
        when(delegate.update("a.mp3", Map.of("title", "Updated"))).thenReturn(updated);

        Music result = cached.update("a.mp3", Map.of("title", "Updated"));

        assertEquals("Updated", result.getMetadata().getTitle());
        verify(cache).put("a.mp3", updated.toTagMap());
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
