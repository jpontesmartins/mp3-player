package com.mp3player.lyrics.domain.repository;

import com.mp3player.lyrics.domain.model.Lyric;
import com.mp3player.player.domain.model.MusicFile;

import java.util.Optional;

/**
 * Repositório para persistir letras de músicas. As implementações decidem onde
 * o texto é armazenado (atualmente um TXT ao lado do áudio); trocar para um
 * banco de dados depois exige apenas uma nova implementação.
 */
public interface LyricRepository {

    /**
     * Carrega a letra em cache para a música informada, caso tenha sido salva anteriormente.
     *
     * @param musicPath caminho absoluto do arquivo de áudio
     * @return um {@link Optional} contendo a letra encontrada, ou vazio se não existir
     */
    Optional<Lyric> find(String musicPath);

    /**
     * Persiste/salva a letra. O repositório usa os metadados da música para
     * escolher a localização e o nome do arquivo de armazenamento.
     *
     * @param lyric letra a ser persistida
     * @param musicFile música com metadados para determinar o caminho de armazenamento
     */
    void save(Lyric lyric, MusicFile musicFile);

    /**
     * Indica se já existe uma letra em cache para a música.
     *
     * @param musicPath caminho absoluto do arquivo de áudio
     * @return {@code true} se existe, {@code false} caso contrário
     */
    boolean exists(String musicPath);

    /**
     * Remove a letra em cache (arquivo TXT) para a música informada.
     *
     * @param musicPath caminho absoluto do arquivo de áudio
     */
    void delete(String musicPath);
}