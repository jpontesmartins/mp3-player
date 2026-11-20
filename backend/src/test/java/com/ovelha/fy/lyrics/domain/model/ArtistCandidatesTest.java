package com.ovelha.fy.lyrics.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testes de {@link ArtistCandidates}.
 */
class ArtistCandidatesTest {

    @Test
    void tagFirstThenArtistsFromFilename() {
        List<String> result = ArtistCandidates.from("Gilberto Gil",
                "C:\\m\\1-11 Gilberto Gil e Os Mutantes - Bat Macumba.mp3");
        assertEquals(List.of("Gilberto Gil", "Os Mutantes"), result);
    }

    @Test
    void multipleArtistsAreSplitAndLimited() {
        List<String> result = ArtistCandidates.from("Gilberto Gil",
                "C:\\m\\1-05 Gilberto Gil, Caetano Veloso, Gal Costa e Os Mutantes - Parque Industrial.mp3");
        assertEquals(List.of("Gilberto Gil", "Caetano Veloso", "Gal Costa", "Os Mutantes"), result);
    }

    @Test
    void withoutTagUsesFilenameArtist() {
        assertEquals(List.of("Artista"), ArtistCandidates.from(null, "C:\\m\\Artista - Musica.mp3"));
    }

    @Test
    void filenameWithoutSeparatorKeepsOnlyTag() {
        assertEquals(List.of("Iggy Pop"), ArtistCandidates.from("Iggy Pop", "C:\\m\\04 The Passenger.mp3"));
    }

    @Test
    void duplicatesIgnoreCaseAndAccents() {
        assertEquals(List.of("Nara Leão"), ArtistCandidates.from("Nara Leão", "C:\\m\\nara leao - Insensatez.mp3"));
    }

    @Test
    void noTagNoArtistReturnsEmpty() {
        assertEquals(List.of(), ArtistCandidates.from("", "C:\\m\\Musica.mp3"));
    }
}
