package com.mp3player.lyrics.infrastructure;

import com.mp3player.lyrics.domain.model.Lyric;
import com.mp3player.player.domain.model.Music;
import com.mp3player.metadata.domain.port.Id3Codec;
import com.mp3player.lyrics.domain.repository.LyricRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mp3player.shared.domain.util.MusicFileNaming;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Implementação baseada em arquivos de {@link LyricRepository}. As letras são
 * armazenadas como um arquivo TXT na pasta do álbum (pai) da música, nomeado
 * "{artista} - {título}.txt" usando as tags ID3, com fallback para o nome do
 * arquivo quando as tags estão ausentes.
 */
@Repository
public class FileLyricRepository implements LyricRepository {

    private static final Logger log = LoggerFactory.getLogger(FileLyricRepository.class);

    private final Id3Codec id3Codec;

    /**
     * Cria uma nova instância do repositório de letras baseado em arquivos.
     *
     * @param id3Codec codec para leitura de metadados ID3
     */
    public FileLyricRepository(Id3Codec id3Codec) {
        this.id3Codec = id3Codec;
    }

    /**
     * Busca a letra em cache a partir do arquivo TXT correspondente.
     *
     * @param musicPath caminho absoluto do arquivo de áudio
     * @return {@link Optional} com a letra encontrada, ou vazio se não existir
     */
    @Override
    public Optional<Lyric> find(String musicPath) {
        Path file = resolveTxtFile(musicPath);
        if (file == null || !Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Lyric(musicPath, Files.readString(file, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            log.error("[Lyrics] Erro ao ler letra de {}", musicPath, e);
            return Optional.empty();
        }
    }

    /**
     * Verifica se existe um arquivo TXT de letra para a música informada.
     *
     * @param musicPath caminho absoluto do arquivo de áudio
     * @return {@code true} se o arquivo existe, {@code false} caso contrário
     */
    @Override
    public boolean exists(String musicPath) {
        Path file = resolveTxtFile(musicPath);
        return file != null && Files.exists(file);
    }

    /**
     * Salva a letra em um arquivo TXT na pasta do álbum da música.
     *
     * @param lyric letra a ser persistida
     * @param music música com metadados para determinar o nome do arquivo
     */
    @Override
    public void save(Lyric lyric, Music music) {
        Path file = resolveTxtFile(lyric.getMusicPath());
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, lyric.getText(), StandardCharsets.UTF_8);
            log.info("[Lyrics] Letra salva em {}", file.toAbsolutePath());
        } catch (IOException e) {
            log.error("[Lyrics] Erro ao salvar letra de {}", lyric.getMusicPath(), e);
        }
    }

    /**
     * Remove o arquivo TXT de letra correspondente à música informada.
     *
     * @param musicPath caminho absoluto do arquivo de áudio
     */
    @Override
    public void delete(String musicPath) {
        Path file = resolveTxtFile(musicPath);
        if (file == null) return;
        try {
            Files.deleteIfExists(file);
            log.info("[Lyrics] Letra removida de {}", musicPath);
        } catch (IOException e) {
            log.error("[Lyrics] Erro ao remover letra de {}", musicPath, e);
        }
    }

    /** Resolve o arquivo TXT de letra para o áudio informado, na pasta do álbum. */
    private Path resolveTxtFile(String musicPath) {
        Path parent = Paths.get(musicPath).getParent();
        if (parent == null) return null;
        String fileName = resolveTxTFileName(musicPath);
        return parent.resolve(fileName);
    }

    /**
     * Nome do arquivo de letra: usa as tags ID3 (artista, título) se presentes;
     * senão infere do nome do arquivo MP3 ("Artista - Música" ou "Música").
     */
    private String resolveTxTFileName(String musicPath) {
        String id3Artist = id3Tag(musicPath, true);
        String id3Title = id3Tag(musicPath, false);

        if (!id3Artist.isBlank() && !id3Title.isBlank()) {
            return sanitizeFileName(id3Artist + " - " + id3Title) + ".txt";
        }

        String base = MusicFileNaming.baseName(musicPath);
        int dash = base.indexOf(" - ");
        if (dash > 0 && id3Artist.isBlank() && id3Title.isBlank()) {
            return sanitizeFileName(base) + ".txt";
        }

        String artist = id3Artist.isBlank() ? "" : id3Artist;
        String title = id3Title.isBlank() ? base : id3Title;
        return fileNameFor(artist, title);
    }

    /**
     * Lê uma tag ID3 do arquivo de música.
     *
     * @param musicPath caminho absoluto do arquivo de áudio
     * @param artist {@code true} para retornar o artista, {@code false} para o título
     * @return valor da tag ID3, ou string vazia se não encontrada ou ocorrer erro
     */
    private String id3Tag(String musicPath, boolean artist) {
        try {
            Music music = id3Codec.read(musicPath);
            String value = artist ? music.getMetadata().getArtist() : music.getMetadata().getTitle();
            return value == null ? "" : value.trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Gera o nome do arquivo TXT de letra a partir do artista e título.
     *
     * @param artist nome do artista (pode ser vazio ou nulo)
     * @param title título da música
     * @return nome do arquivo TXT sanitizado
     */
    private static String fileNameFor(String artist, String title) {
        String prefix = artist == null || artist.isBlank() ? "" : artist.trim() + " - ";
        return sanitizeFileName(prefix + (title == null ? "" : title.trim())) + ".txt";
    }

    /**
     * Remove caracteres inválidos para nomes de arquivo.
     *
     * @param s string original
     * @return string sanitizada, com caracteres inválidos substituídos por underscore
     */
    private static String sanitizeFileName(String s) {
        return s.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }


}