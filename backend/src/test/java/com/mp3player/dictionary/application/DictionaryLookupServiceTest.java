package com.mp3player.dictionary.application;

import com.mp3player.dictionary.domain.model.DictionaryLookupResult;
import com.mp3player.dictionary.domain.port.DictionarySource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DictionaryLookupServiceTest {

    @Mock
    DictionarySource ptSource;
    @Mock
    DictionarySource enSource;

    @Test
    void lookupReturnsResultForSupportedLanguage() {
        when(ptSource.language()).thenReturn("pt");
        when(ptSource.lookup("casa")).thenReturn(new DictionaryLookupResult("casa", "Priberam", "pt", "moradia"));

        DictionaryLookupService service = new DictionaryLookupService(List.of(ptSource));
        DictionaryLookupResult result = service.lookup("casa", "pt");

        assertNotNull(result);
        assertEquals("casa", result.word());
        assertEquals("Priberam", result.source());
        verify(ptSource).lookup("casa");
    }

    @Test
    void lookupReturnsNullForUnsupportedLanguage() {
        when(ptSource.language()).thenReturn("pt");

        DictionaryLookupService service = new DictionaryLookupService(List.of(ptSource));
        DictionaryLookupResult result = service.lookup("casa", "fr");

        assertNull(result);
        verify(ptSource, never()).lookup(anyString());
    }

    @Test
    void supportedLanguagesReturnsCorrectList() {
        when(ptSource.language()).thenReturn("pt");
        when(enSource.language()).thenReturn("en");

        DictionaryLookupService service = new DictionaryLookupService(List.of(ptSource, enSource));
        List<String> languages = service.supportedLanguages();

        assertEquals(2, languages.size());
        assertTrue(languages.contains("pt"));
        assertTrue(languages.contains("en"));
    }

    @Test
    void constructorIndexesSourcesByLanguage() {
        when(ptSource.language()).thenReturn("pt");
        when(enSource.language()).thenReturn("en");

        DictionaryLookupService service = new DictionaryLookupService(List.of(ptSource, enSource));

        DictionaryLookupResult ptResult = service.lookup("oi", "pt");
        DictionaryLookupResult enResult = service.lookup("hello", "en");

        verify(ptSource).lookup("oi");
        verify(enSource).lookup("hello");
    }
}
