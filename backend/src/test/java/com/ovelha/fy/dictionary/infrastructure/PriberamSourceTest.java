package com.ovelha.fy.dictionary.infrastructure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriberamSourceTest {

    private final PriberamSource source = new PriberamSource();

    @Test
    void languageReturnsPt() {
        assertEquals("pt", source.language());
    }

    @Test
    void sourceNameReturnsPriberam() {
        assertEquals("Priberam", source.sourceName());
    }

    @Test
    void lookupReturnsNullForBlankWord() {
        assertNull(source.lookup(""));
        assertNull(source.lookup("   "));
        assertNull(source.lookup(null));
    }
}
