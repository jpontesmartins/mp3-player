package com.ovelha.fy.player.music.infrastructure;

import com.ovelha.fy.player.music.domain.model.CoverImage;
import com.ovelha.fy.player.music.domain.port.AlbumCoverSearcher;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompositeCoverSearcherTest {

    @Test
    void findCoverReturnsFirstSuccessfulResult() throws IOException {
        AlbumCoverSearcher first = mock(AlbumCoverSearcher.class);
        AlbumCoverSearcher second = mock(AlbumCoverSearcher.class);
        CoverImage expected = new CoverImage(new byte[]{1, 2}, "image/jpeg");

        when(first.findCover("query")).thenReturn(expected);
        when(second.findCover("query")).thenReturn(null);

        CompositeCoverSearcher composite = new CompositeCoverSearcher(List.of(first, second));
        CoverImage result = composite.findCover("query");

        assertEquals(expected, result);
        verify(first).findCover("query");
        verify(second, never()).findCover("query");
    }

    @Test
    void findCoverFallsBackToSecondWhenFirstReturnsNull() throws IOException {
        AlbumCoverSearcher first = mock(AlbumCoverSearcher.class);
        AlbumCoverSearcher second = mock(AlbumCoverSearcher.class);
        CoverImage expected = new CoverImage(new byte[]{1, 2}, "image/jpeg");

        when(first.findCover("query")).thenReturn(null);
        when(second.findCover("query")).thenReturn(expected);

        CompositeCoverSearcher composite = new CompositeCoverSearcher(List.of(first, second));
        CoverImage result = composite.findCover("query");

        assertEquals(expected, result);
        verify(first).findCover("query");
        verify(second).findCover("query");
    }

    @Test
    void findCoverFallsBackWhenFirstReturnsEmpty() throws IOException {
        AlbumCoverSearcher first = mock(AlbumCoverSearcher.class);
        AlbumCoverSearcher second = mock(AlbumCoverSearcher.class);
        CoverImage expected = new CoverImage(new byte[]{1, 2}, "image/jpeg");

        when(first.findCover("query")).thenReturn(new CoverImage(new byte[0], "image/jpeg"));
        when(second.findCover("query")).thenReturn(expected);

        CompositeCoverSearcher composite = new CompositeCoverSearcher(List.of(first, second));
        CoverImage result = composite.findCover("query");

        assertEquals(expected, result);
    }

    @Test
    void findCoverReturnsNullWhenAllReturnNull() throws IOException {
        AlbumCoverSearcher first = mock(AlbumCoverSearcher.class);
        AlbumCoverSearcher second = mock(AlbumCoverSearcher.class);

        when(first.findCover("query")).thenReturn(null);
        when(second.findCover("query")).thenReturn(null);

        CompositeCoverSearcher composite = new CompositeCoverSearcher(List.of(first, second));
        CoverImage result = composite.findCover("query");

        assertNull(result);
        verify(first).findCover("query");
        verify(second).findCover("query");
    }

    @Test
    void findCoverPropagatesIOException() throws IOException {
        AlbumCoverSearcher first = mock(AlbumCoverSearcher.class);
        when(first.findCover("query")).thenThrow(new IOException("network error"));

        CompositeCoverSearcher composite = new CompositeCoverSearcher(List.of(first));

        assertThrows(IOException.class, () -> composite.findCover("query"));
    }
}
