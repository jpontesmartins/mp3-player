package com.mp3player.domain.repository;

import com.mp3player.domain.model.Lyric;
import com.mp3player.domain.model.Music;

import java.util.Optional;

/**
 * Repositório para persistir letras de músicas. As implementações decidem onde
 * o texto é armazenado (atualmente um TXT ao lado do áudio); trocar para um
 * banco de dados depois exige apenas uma nova implementação.
 */
public interface LyricRepository {

    /**
     * Carrega a letra em cache para a música informada, caso tenha sido salva anteriormente.
     */
    Optional<Lyric> find(String musicPath);

    /**
     * Persiste/salva a letra. O repositório usa os metadados da música para
     * escolher a localização e o nome do arquivo de armazenamento.
     */
    void save(Lyric lyric, Music music);

    /** Indica se já existe uma letra em cache para a música. */
    boolean exists(String musicPath);
}