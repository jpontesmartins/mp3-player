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
    void trackNumberOnlyPrefixIsNotACandidate() {
        assertEquals(List.of("Nara Leão"),
                ArtistCandidates.from("Nara Leão", "C:\\m\\05 - O Que Será.mp3"));
        assertEquals(List.of(), ArtistCandidates.from("", "C:\\m\\06 - Angry Chair.mp3"));
    }

    @Test
    void tagEqualToFullFilenameSegmentIsNotSplit() {
        assertEquals(List.of("Earth, Wind & Fire"),
                ArtistCandidates.from("Earth, Wind & Fire", "C:\\m\\Earth, Wind & Fire - September.mp3"));
    }

    @Test
    void withoutTagFullSegmentComesBeforeFragments() {
        assertEquals(List.of("Earth, Wind & Fire", "Earth", "Wind", "Fire"),
                ArtistCandidates.from(null, "C:\\m\\Earth, Wind & Fire - September.mp3"));
    }

    @Test
    void noTagNoArtistReturnsEmpty() {
        assertEquals(List.of(), ArtistCandidates.from("", "C:\\m\\Musica.mp3"));
    }

    @Test
    void numericArtistNamesAreKept() {
        assertEquals(List.of("50 Cent"), ArtistCandidates.from(null, "C:/m/50 Cent - In Da Club.mp3"));
        assertEquals(List.of("2 Unlimited"), ArtistCandidates.from(null, "C:/m/2 Unlimited - No Limit.mp3"));
    }

    @Test
    void trackPrefixVariantsAreStripped() {
        assertEquals(List.of("Artista"), ArtistCandidates.from(null, "C:/m/01 Artista - Musica.mp3"));
        assertEquals(List.of("Artista"), ArtistCandidates.from(null, "C:/m/1-05 Artista - Musica.mp3"));
        assertEquals(List.of("Artista"), ArtistCandidates.from(null, "C:/m/01. Artista - Musica.mp3"));
    }
}
