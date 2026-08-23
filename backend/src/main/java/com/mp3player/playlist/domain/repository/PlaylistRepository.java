package com.mp3player.playlist.domain.repository;

import com.mp3player.playlist.domain.model.Playlist;

import java.util.List;

/**
 * Port de repositório para persistir e carregar playlists. A implementação
 * atual armazena cada playlist como um arquivo TXT de caminhos absolutos, mas
 * uma implementação poderia usar um banco de dados sem alterar o domínio.
 */
public interface PlaylistRepository {

    /** Nomes de todas as playlists salvas. */
    List<String> list();

    /** Carrega os caminhos ordenados das músicas de uma playlist. */
    List<String> load(String name);

    /** Cria ou sobrescreve uma playlist. */
    void save(Playlist playlist);

    /** Exclui uma playlist. */
    void delete(String name);

    /** Renomeia uma playlist existente. */
    void rename(String currentName, String newName);
}