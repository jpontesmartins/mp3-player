package com.mp3player.playlist.application;

import com.mp3player.playlist.domain.model.Playlist;
import com.mp3player.playlist.domain.port.MusicScanner;
import com.mp3player.playlist.domain.repository.PlaylistRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Service da aplicação para o módulo de playlist: carregar, criar, editar,
 * listar, excluir e renomear playlists, além do scan da pasta física.
 */
@Service
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final MusicScanner musicScanner;

    /**
     * Construtor do service de playlist.
     *
     * @param playlistRepository repositório de persistência de playlists.
     * @param musicScanner       scanner de músicas em diretórios.
     */
    public PlaylistService(PlaylistRepository playlistRepository, MusicScanner musicScanner) {
        this.playlistRepository = playlistRepository;
        this.musicScanner = musicScanner;
    }

    /**
     * Escaneia uma pasta física e retorna os caminhos absolutos de todas as músicas encontradas.
     *
     * @param folderPath caminho absoluto da pasta a ser escaneada.
     * @return lista de caminhos absolutos dos arquivos de música.
     * @throws IOException se a pasta não existir ou não puder ser lida.
     */
    public List<String> scanFolder(String folderPath) throws IOException {
        return musicScanner.scanFolder(folderPath).stream().map(m -> m.getPath()).toList();
    }

    /**
     * Lista as playlists salvas.
     *
     * @return lista ordenada de nomes de playlists.
     */
    public List<String> list() {
        return playlistRepository.list();
    }

    /**
     * Carrega os caminhos das músicas de uma playlist.
     *
     * @param name nome da playlist.
     * @return lista de caminhos absolutos das músicas.
     */
    public List<String> load(String name) {
        return playlistRepository.load(name);
    }

    /**
     * Cria ou sobrescreve uma playlist com os caminhos informados.
     *
     * @param name      nome da playlist.
     * @param songPaths lista de caminhos absolutos das músicas.
     */
    public void createOrUpdate(String name, List<String> songPaths) {
        playlistRepository.save(new Playlist(name, songPaths));
    }

    /**
     * Exclui uma playlist pelo nome.
     *
     * @param name nome da playlist a ser excluída.
     */
    public void delete(String name) {
        playlistRepository.delete(name);
    }

    /**
     * Renomeia uma playlist existente.
     *
     * @param currentName nome atual da playlist.
     * @param newName     novo nome da playlist.
     */
    public void rename(String currentName, String newName) {
        playlistRepository.rename(currentName, newName);
    }
}