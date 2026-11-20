package com.ovelha.fy.lyrics.domain.port;

import java.util.List;
import java.util.Optional;

/**
 * Port para buscar o texto da letra em uma fonte web.
 */
public interface LyricsScraper {

    /**
     * Busca a letra tentando cada artista candidato, em ordem.
     *
     * @param artistCandidates nomes de artista a tentar (o primeiro é o principal)
     * @param title título da música
     * @return o texto da letra, ou vazio se nenhuma fonte/candidato a encontrou
     */
    Optional<String> fetch(List<String> artistCandidates, String title);
}
