package com.mp3player.music.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoverImageTest {

    @Test
    void isEmptyWhenBytesNull() {
        assertTrue(new CoverImage(null, "image/jpeg").isEmpty());
    }

    @Test
    void isEmptyWhenBytesEmpty() {
        assertTrue(new CoverImage(new byte[0], "image/png").isEmpty());
    }

    @Test
    void isNotEmptyWhenBytesPresent() {
        assertFalse(new CoverImage(new byte[] { 1, 2, 3 }, "image/webp").isEmpty());
    }

    @Test
    void recordEqualsUsesAllComponents() {
        byte[] bytes = new byte[] { 1 };
        CoverImage a = new CoverImage(bytes, "image/jpeg");
        CoverImage b = new CoverImage(bytes, "image/jpeg");
        CoverImage differentContentType = new CoverImage(bytes, "image/png");
        assertEquals(a, b);
        assertNotEquals(a, differentContentType);
    }

    @Test
    void distinctByteArraysMakeImagesDifferent() {
        CoverImage a = new CoverImage(new byte[] { 1 }, "image/jpeg");
        CoverImage b = new CoverImage(new byte[] { 1 }, "image/jpeg");
        assertNotEquals(a, b);
    }

    @Test
    void exposesContentType() {
        CoverImage image = new CoverImage(new byte[] { 1 }, "image/gif");
        assertEquals("image/gif", image.contentType());
    }
}