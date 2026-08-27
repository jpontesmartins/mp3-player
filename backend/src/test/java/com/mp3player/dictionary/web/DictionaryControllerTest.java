package com.mp3player.dictionary.web;

import com.mp3player.dictionary.application.DictionaryLookupService;
import com.mp3player.dictionary.domain.model.DictionaryLookupResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DictionaryControllerTest {

    @Mock
    DictionaryLookupService lookupService;

    @Test
    void lookupReturns200WithResult() {
        when(lookupService.lookup("casa", "pt"))
                .thenReturn(new DictionaryLookupResult("casa", "Priberam", "pt", "moradia"));
        DictionaryController controller = new DictionaryController(lookupService);

        var response = controller.lookup(new DictionaryController.DictionaryLookupRequest("casa", "pt"));

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("casa", response.getBody().word());
    }

    @Test
    void lookupReturns400WhenWordIsBlank() {
        DictionaryController controller = new DictionaryController(lookupService);

        var response = controller.lookup(new DictionaryController.DictionaryLookupRequest("  ", "pt"));

        assertEquals(400, response.getStatusCode().value());
        verify(lookupService, never()).lookup(anyString(), anyString());
    }

    @Test
    void lookupReturns400WhenLanguageIsBlank() {
        DictionaryController controller = new DictionaryController(lookupService);

        var response = controller.lookup(new DictionaryController.DictionaryLookupRequest("casa", "  "));

        assertEquals(400, response.getStatusCode().value());
        verify(lookupService, never()).lookup(anyString(), anyString());
    }

    @Test
    void lookupReturns404WhenSourceNotFound() {
        when(lookupService.lookup("casa", "fr")).thenReturn(null);
        DictionaryController controller = new DictionaryController(lookupService);

        var response = controller.lookup(new DictionaryController.DictionaryLookupRequest("casa", "fr"));

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void languagesReturns200WithList() {
        when(lookupService.supportedLanguages()).thenReturn(List.of("pt", "en"));
        DictionaryController controller = new DictionaryController(lookupService);

        var response = controller.languages();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, response.getBody().size());
        assertTrue(response.getBody().contains("pt"));
        assertTrue(response.getBody().contains("en"));
    }
}
