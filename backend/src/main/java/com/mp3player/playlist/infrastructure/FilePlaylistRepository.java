package com.mp3player.playlist.infrastructure;

import com.mp3player.playlist.domain.model.Playlist;
import com.mp3player.playlist.domain.repository.PlaylistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Implementação baseada em arquivos de {@link PlaylistRepository}. Cada playlist
 * é um arquivo TXT no diretório de playlists do usuário, com um caminho absoluto por linha.
 */
@Repository
public class FilePlaylistRepository implements PlaylistRepository {

    private static final Logger log = LoggerFactory.getLogger(FilePlaylistRepository.class);

    private final Path baseDir;

    /**
     * Construtor padrão que usa o diretório padrão de playlists do usuário.
     */
    public FilePlaylistRepository() {
        this(Paths.get(System.getProperty("user.home"), ".mp3-player", "playlists"));
    }

    /**
     * Construtor para testes com diretório personalizado.
     *
     * @param baseDir diretório base onde as playlists serão armazenadas.
     */
    FilePlaylistRepository(Path baseDir) {
        this.baseDir = baseDir;
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            log.error("[Playlist] Não foi possível criar diretório de playlists: {}", baseDir, e);
        }
    }

    /**
     * Lista os nomes de todas as playlists salvas no diretório base.
     *
     * @return lista ordenada de nomes de playlists, ou lista vazia em caso de erro.
     */
    @Override
    public List<String> list() {
        try (Stream<Path> stream = Files.list(baseDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".txt"))
                    .map(p -> p.getFileName().toString().replaceFirst("(?i)\\.txt$", ""))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            log.error("[Playlist] Erro ao listar playlists", e);
            return List.of();
        }
    }

    /**
     * Carrega os caminhos ordenados das músicas de uma playlist.
     *
     * @param name nome da playlist.
     * @return lista de caminhos absolutos das músicas, ou lista vazia se não existir ou houver erro.
     */
    @Override
    public List<String> load(String name) {
        Path file = fileFor(name);
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8)
                    .stream()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty())
                    .toList();
        } catch (IOException e) {
            log.error("[Playlist] Erro ao carregar playlist {}", name, e);
            return List.of();
        }
    }

    /**
     * Salva uma playlist em arquivo TXT no diretório base.
     *
     * @param playlist playlist a ser salva.
     * @throws IllegalStateException se ocorrer erro de I/O ao gravar o arquivo.
     */
    @Override
    public void save(Playlist playlist) {
        Path file = fileFor(playlist.getName());
        try {
            Files.write(file, playlist.getSongPaths(), StandardCharsets.UTF_8);
            log.info("[Playlist] Playlist salva: {} ({} músicas)", playlist.getName(), playlist.getSongPaths().size());
        } catch (IOException e) {
            throw new IllegalStateException("Error saving playlist " + playlist.getName(), e);
        }
    }

    /**
     * Exclui uma playlist pelo nome.
     *
     * @param name nome da playlist a ser excluída.
     * @throws IllegalStateException se ocorrer erro de I/O ao excluir o arquivo.
     */
    @Override
    public void delete(String name) {
        try {
            Files.deleteIfExists(fileFor(name));
            log.info("[Playlist] Playlist removida: {}", name);
        } catch (IOException e) {
            throw new IllegalStateException("Error deleting playlist " + name, e);
        }
    }

    /**
     * Renomeia uma playlist existente.
     *
     * @param currentName nome atual da playlist.
     * @param newName     novo nome da playlist.
     * @throws IllegalArgumentException se a playlist não existir.
     * @throws IllegalStateException    se ocorrer erro de I/O ao renomear o arquivo.
     */
    @Override
    public void rename(String currentName, String newName) {
        Path from = fileFor(currentName);
        if (!Files.exists(from)) {
            throw new IllegalArgumentException("Playlist not found: " + currentName);
        }
        try {
            Files.move(from, fileFor(newName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("[Playlist] Playlist renomeada: {} -> {}", currentName, newName);
        } catch (IOException e) {
            throw new IllegalStateException("Error renaming playlist", e);
        }
    }

    /**
     * Sanitiza o nome da playlist para uso como nome de arquivo, removendo caracteres inválidos.
     *
     * @param name nome original da playlist.
     * @return nome sanitizado; retorna "playlist" se o resultado for vazio.
     */
    String sanitize(String name) {
        String cleaned = name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        return cleaned.isEmpty() ? "playlist" : cleaned;
    }

    private Path fileFor(String name) {
        return baseDir.resolve(sanitize(name) + ".txt");
    }
}