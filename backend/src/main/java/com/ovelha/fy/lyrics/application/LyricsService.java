package com.ovelha.fy.lyrics.application;

import com.ovelha.fy.lyrics.domain.model.ArtistCandidates;
import com.ovelha.fy.lyrics.domain.model.Lyric;
import com.ovelha.fy.player.domain.model.MusicFile;
import com.ovelha.fy.player.music.domain.port.Id3Codec;
import com.ovelha.fy.lyrics.domain.port.LyricsScraper;
import com.ovelha.fy.lyrics.domain.repository.LyricRepository;
import com.ovelha.fy.shared.domain.util.MusicFileNaming;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service da aplicação para o módulo de letras: lê letras em cache, busca letras
 * na web via port {@link LyricsScraper} e as persiste através do {@link LyricRepository}.
 */
@Service
public class LyricsService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LyricsService.class);

    private final Id3Codec id3Codec;
    private final LyricsScraper lyricsScraper;
    private final LyricRepository lyricRepository;

    /**
     * Cria uma nova instância do serviço de letras.
     *
     * @param id3Codec codec para leitura de metadados ID3
     * @param lyricsScraper scraper para busca de letras na web
     * @param lyricRepository repositório para persistência de letras
     */
    public LyricsService(Id3Codec id3Codec, LyricsScraper lyricsScraper, LyricRepository lyricRepository) {
        this.id3Codec = id3Codec;
        this.lyricsScraper = lyricsScraper;
        this.lyricRepository = lyricRepository;
    }

    /**
     * Retorna a letra em cache para o áudio informado, ou {@code null} se não houver.
     *
     * @param musicPath caminho absoluto do arquivo de áudio
     * @return texto da letra em cache, ou {@code null} se não encontrada
     */
    public String getCached(String musicPath) {
        return lyricRepository.find(musicPath)
                .map(Lyric::getText)
                .filter(text -> !Lyric.isNotFoundMessage(text))
                .orElse(null);
    }

    /**
     * Retorna a letra em cache ou busca na web, salvando o resultado.
     *
     * @param musicPath caminho absoluto do arquivo de áudio
     * @return texto da letra encontrada ou mensagem amigável (esta última não é persistida)
     */
    public String get(String musicPath) {
        String cached = getCached(musicPath);
        if (cached != null) return cached;

        MusicFile musicFile = musicFor(musicPath);
        String artist = artistOrFilename(musicFile);
        String title = titleOrFilename(musicFile);
        List<String> candidates = ArtistCandidates.from(blank(musicFile.getMetadata().artist()), musicPath);

        Optional<String> found = fetchSafely(candidates, title);
        if (found.isEmpty()) {
            return Lyric.notFoundMessage(artist, title);
        }
        lyricRepository.save(new Lyric(musicPath, found.get()), musicFile);
        return found.get();
    }

    /**
     * Busca a letra na web via scraper; falhas viram resultado vazio (nada é persistido).
     *
     * @param candidates artistas candidatos
     * @param title título da música
     * @return letra encontrada ou vazio
     */
    private Optional<String> fetchSafely(List<String> candidates, String title) {
        try {
            return lyricsScraper.fetch(candidates, title);
        } catch (Exception e) {
            log.warn("[Lyrics] Falha ao buscar letra de \"{}\": {}", title, e.toString(), e);
            return Optional.empty();
        }
    }

    /**
     * Persiste o texto informado como a letra da música.
     *
     * @param musicPath caminho absoluto do arquivo de áudio
     * @param text texto da letra a ser salva
     */
    public void save(String musicPath, String text) {
        lyricRepository.save(new Lyric(musicPath, text), musicFor(musicPath));
    }

    /**
     * Remove a letra em cache para a música informada.
     *
     * @param musicPath caminho absoluto do arquivo de áudio
     */
    public void delete(String musicPath) {
        lyricRepository.delete(musicPath);
    }

    /**
     * Lê os metadados ID3 do arquivo de música.
     *
     * @param musicPath caminho absoluto do arquivo de áudio
     * @return agregado de música com os metadados lidos
     */
    private MusicFile musicFor(String musicPath) {
        return id3Codec.read(musicPath);
    }

    /**
     * Retorna o artista a partir das tags ID3, ou infere do nome do arquivo.
     *
     * @param musicFile música com metadados
     * @return nome do artista
     */
    static String artistOrFilename(MusicFile musicFile) {
        String artist = blank(musicFile.getMetadata().artist());
        if (!artist.isEmpty()) return artist;
        return MusicFileNaming.artistFromFilename(musicFile.getPath());
    }

    /**
     * Retorna o título a partir das tags ID3, ou infere do nome do arquivo.
     *
     * @param musicFile música com metadados
     * @return título da música
     */
    static String titleOrFilename(MusicFile musicFile) {
        String title = blank(musicFile.getMetadata().title());
        if (!title.isEmpty()) return title;
        return MusicFileNaming.titleFromFilename(musicFile.getPath());
    }

    /**
     * Retorna a string informada ou string vazia se for nula, removendo espaços em branco.
     *
     * @param s string de entrada
     * @return string não nula, sem espaços nas extremidades
     */
    private static String blank(String s) {
        return s == null ? "" : s.trim();
    }
}