package com.mp3player.application.lyrics;

import com.mp3player.domain.model.Lyric;
import com.mp3player.domain.model.Music;
import com.mp3player.domain.port.Id3Codec;
import com.mp3player.domain.port.LyricsScraper;
import com.mp3player.domain.repository.LyricRepository;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;

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
        if (!lyricRepository.exists(musicPath)) return null;
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

    static String artistOrFilename(Music music) {
        String artist = blank(music.getMetadata().getArtist());
        if (!artist.isEmpty()) return artist;
        return artistFromFilename(baseName(music.getPath()));
    }

    static String titleOrFilename(Music music) {
        String title = blank(music.getMetadata().getTitle());
        if (!title.isEmpty()) return title;
        return titleFromFilename(baseName(music.getPath()));
    }

    private static String baseName(String path) {
        String name = Paths.get(path).getFileName().toString();
        if (name.toLowerCase().endsWith(".mp3")) {
            name = name.substring(0, name.length() - 4);
        }
        return name;
    }

    private static String artistFromFilename(String name) {
        int dash = name.indexOf(" - ");
        return dash > 0 ? name.substring(0, dash).trim() : "";
    }

    private static String titleFromFilename(String name) {
        int dash = name.indexOf(" - ");
        return dash > 0 ? name.substring(dash + 3).trim() : name.trim();
    }

    private static String blank(String s) {
        return s == null ? "" : s.trim();
    }
}