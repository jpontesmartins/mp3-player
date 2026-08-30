package com.ovelha.fy.dictionary.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DictionaryLookupResultTest {

    @Test
    void recordEqualityWithSameValues() {
        var a = new DictionaryLookupResult("casa", "Priberam", "pt", "moradia");
        var b = new DictionaryLookupResult("casa", "Priberam", "pt", "moradia");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void recordNotEqualWithDifferentValues() {
        var a = new DictionaryLookupResult("casa", "Priberam", "pt", "moradia");
        var b = new DictionaryLookupResult("casa", "Priberam", "pt", "construção");

        assertNotEquals(a, b);
    }

    @Test
    void recordComponentsAreAccessible() {
        var result = new DictionaryLookupResult("casa", "Priberam", "pt", "moradia");

        assertEquals("casa", result.word());
        assertEquals("Priberam", result.source());
        assertEquals("pt", result.language());
        assertEquals("moradia", result.meanings());
    }
}
