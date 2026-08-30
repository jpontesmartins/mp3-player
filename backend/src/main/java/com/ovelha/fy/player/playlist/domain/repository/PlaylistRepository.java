package com.ovelha.fy.player.playlist.domain.repository;

import com.ovelha.fy.player.playlist.domain.model.Playlist;

import java.util.List;

/**
 * Port de repositório para persistir e carregar playlists. A implementação
 * atual armazena cada playlist como um arquivo TXT de caminhos absolutos, mas
 * uma implementação poderia usar um banco de dados sem alterar o domínio.
 */
public interface PlaylistRepository {

    /**
     * Lista os nomes de todas as playlists salvas.
     *
     * @return lista ordenada de nomes de playlists.
     */
    List<String> list();

    /**
     * Carrega os caminhos ordenados das músicas de uma playlist.
     *
     * @param name nome da playlist.
     * @return lista de caminhos absolutos das músicas.
     */
    List<String> load(String name);

    /**
     * Cria ou sobrescreve uma playlist.
     *
     * @param playlist playlist a ser salva.
     */
    void save(Playlist playlist);

    /**
     * Exclui uma playlist pelo nome.
     *
     * @param name nome da playlist a ser excluída.
     */
    void delete(String name);

    /**
     * Renomeia uma playlist existente.
     *
     * @param currentName nome atual da playlist.
     * @param newName     novo nome da playlist.
     * @throws IllegalArgumentException se a playlist não existir.
     */
    void rename(String currentName, String newName);
}