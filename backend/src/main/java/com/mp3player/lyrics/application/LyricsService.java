package com.mp3player.lyrics.application;

import com.mp3player.lyrics.domain.model.Lyric;
import com.mp3player.player.domain.model.MusicFile;
import com.mp3player.music.domain.port.Id3Codec;
import com.mp3player.lyrics.domain.port.LyricsScraper;
import com.mp3player.lyrics.domain.repository.LyricRepository;
import com.mp3player.shared.domain.util.MusicFileNaming;
import org.springframework.stereotype.Service;

/**
 * Service da aplicação para o módulo de letras: lê letras em cache, busca letras
 * na web via port {@link LyricsScraper} e as persiste através do {@link LyricRepository}.
 */
@Service
public class LyricsService {

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
        return lyricRepository.find(musicPath).map(Lyric::getText).orElse(null);
    }

    /**
     * Retorna a letra em cache ou busca na web, salvando o resultado.
     *
     * @param musicPath caminho absoluto do arquivo de áudio
     * @return texto da letra encontrada ou mensagem de erro amigável
     */
    public String get(String musicPath) {
        String cached = getCached(musicPath);
        if (cached != null) return cached;

        MusicFile musicFile = musicFor(musicPath);
        String artist = artistOrFilename(musicFile);
        String title = titleOrFilename(musicFile);

        String text = fetchOrFallback(artist, title, musicPath);
        lyricRepository.save(new Lyric(musicPath, text), musicFile);
        return text;
    }

    /**
     * Busca a letra na web via scraper; em caso de falha, retorna mensagem de erro amigável.
     *
     * @param artist nome do artista
     * @param title título da música
     * @param musicPath caminho absoluto do arquivo de áudio (para fallback)
     * @return texto da letra ou mensagem de erro
     */
    private String fetchOrFallback(String artist, String title, String musicPath) {
        try {
            return lyricsScraper.fetch(artist, title);
        } catch (Exception e) {
            return "Letra não encontrada para \"" + title + "\" de " + artist;
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