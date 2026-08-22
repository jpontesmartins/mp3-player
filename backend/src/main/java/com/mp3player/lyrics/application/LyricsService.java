package com.mp3player.lyrics.application;

import com.mp3player.lyrics.domain.model.Lyric;
import com.mp3player.player.domain.model.Music;
import com.mp3player.metadata.domain.port.Id3Codec;
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

    public LyricsService(Id3Codec id3Codec, LyricsScraper lyricsScraper, LyricRepository lyricRepository) {
        this.id3Codec = id3Codec;
        this.lyricsScraper = lyricsScraper;
        this.lyricRepository = lyricRepository;
    }

    /** Retorna a letra em cache para o áudio informado, ou {@code null} se não houver. */
    public String getCached(String musicPath) {
        return lyricRepository.find(musicPath).map(Lyric::getText).orElse(null);
    }

    /** Retorna a letra em cache ou busca na web, salvando o resultado. */
    public String get(String musicPath) {
        String cached = getCached(musicPath);
        if (cached != null) return cached;

        Music music = musicFor(musicPath);
        String artist = artistOrFilename(music);
        String title = titleOrFilename(music);

        String text = fetchOrFallback(artist, title, musicPath);
        lyricRepository.save(new Lyric(musicPath, text), music);
        return text;
    }

    private String fetchOrFallback(String artist, String title, String musicPath) {
        try {
            return lyricsScraper.fetch(artist, title);
        } catch (Exception e) {
            return "Letra não encontrada para \"" + title + "\" de " + artist;
        }
    }

    /** Persiste o texto informado como a letra da música. */
    public void save(String musicPath, String text) {
        lyricRepository.save(new Lyric(musicPath, text), musicFor(musicPath));
    }

    /** Remove a letra em cache para a música informada. */
    public void delete(String musicPath) {
        lyricRepository.delete(musicPath);
    }

    private Music musicFor(String musicPath) {
        return id3Codec.read(musicPath);
    }

    /**
     * Retorna o artista a partir das tags ID3, ou infere do nome do arquivo.
     *
     * @param music música com metadados
     * @return nome do artista
     */
    static String artistOrFilename(Music music) {
        String artist = blank(music.getMetadata().getArtist());
        if (!artist.isEmpty()) return artist;
        return MusicFileNaming.artistFromFilename(music.getPath());
    }

    /**
     * Retorna o título a partir das tags ID3, ou infere do nome do arquivo.
     *
     * @param music música com metadados
     * @return título da música
     */
    static String titleOrFilename(Music music) {
        String title = blank(music.getMetadata().getTitle());
        if (!title.isEmpty()) return title;
        return MusicFileNaming.titleFromFilename(music.getPath());
    }

    private static String blank(String s) {
        return s == null ? "" : s.trim();
    }
}